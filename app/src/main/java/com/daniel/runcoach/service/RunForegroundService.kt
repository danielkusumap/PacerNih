package com.daniel.runcoach.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.daniel.runcoach.R
import com.daniel.runcoach.data.RunPersistence
import com.daniel.runcoach.data.RunSnapshot
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.LocationSample
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.model.WorkoutJson
import com.daniel.runcoach.engine.runner.RunnerPhase
import com.daniel.runcoach.engine.runner.RunnerState
import com.daniel.runcoach.engine.runner.WorkoutRunner
import com.daniel.runcoach.service.location.LocationSource
import com.daniel.runcoach.service.location.LocationSourceFactory
import com.daniel.runcoach.service.voice.RunAnnouncer
import com.daniel.runcoach.ui.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.UUID

/**
 * Foreground service (type `location`) that owns the [WorkoutRunner] and the [LocationSource] while a workout
 * runs, so tracking continues with the screen off (plan P1.3). It also persists track points and a resume
 * snapshot every ~5 s (plan P1.5) so a process death can be recovered.
 *
 * Started in "new run" mode (with a workout) or "resume" mode ([ACTION_RESUME]); stopped only by the user.
 */
class RunForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runner: WorkoutRunner? = null
    private lateinit var locationSource: LocationSource
    private var wakeLock: PowerManager.WakeLock? = null
    private var runJob: Job? = null
    private var announcer: RunAnnouncer? = null
    private var persistence: RunPersistence? = null

    private var sessionId: String = ""
    private var workout: Workout? = null
    private var startedAtElapsedMs: Long = 0L
    private var startedAtEpochMs: Long = 0L
    private var stopping = false
    private var trackIdx = 0
    private var lastFlushMs = 0L
    private val trackBuffer = mutableListOf<com.daniel.runcoach.data.TrackPointEntity>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        persistence = RunPersistence(this)
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRun()
                return START_NOT_STICKY
            }

            ACTION_RESUME -> {
                val target = intent.getStringExtra(EXTRA_SESSION_ID)
                if (target != null) resumeRun(target) else stopSelf()
                return START_NOT_STICKY
            }
        }
        val workoutJson = intent?.getStringExtra(EXTRA_WORKOUT)
        val workout = workoutJson?.let { runCatching { WorkoutJson.decode(it) }.getOrNull() } ?: freeRun()
        startNewRun(workout)
        return START_NOT_STICKY
    }

    private fun startNewRun(workout: Workout) {
        this.workout = workout
        sessionId = UUID.randomUUID().toString()
        startedAtElapsedMs = SystemClock.elapsedRealtime()
        startedAtEpochMs = System.currentTimeMillis()
        launchRun(WorkoutRunner(workout), freshStart = true)
    }

    private fun resumeRun(targetSessionId: String) {
        scope.launch {
            val snapshot = persistence?.loadSnapshot()
            val points = persistence?.loadTrackPoints(targetSessionId).orEmpty()
            if (snapshot == null || snapshot.sessionId != targetSessionId) {
                stopSelf()
                return@launch
            }
            val workout = runCatching { WorkoutJson.decode(snapshot.workoutJson) }.getOrNull()
            if (workout == null) {
                stopSelf()
                return@launch
            }
            sessionId = snapshot.sessionId
            this@RunForegroundService.workout = workout
            startedAtElapsedMs = snapshot.startedAtElapsedMs
            startedAtEpochMs = snapshot.startedAtEpochMs
            val replayed = WorkoutRunner(workout)
            replayed.onStart(snapshot.startedAtElapsedMs)
            points.forEach { point ->
                replayed.onTick(point.timeMs)
                replayed.onSample(RunPersistence.toSample(point))
            }
            trackIdx = points.size
            launchRun(replayed, freshStart = false)
        }
    }

    private fun launchRun(activeRunner: WorkoutRunner, freshStart: Boolean) {
        runner = activeRunner
        locationSource = LocationSourceFactory.create(this)
        lastFlushMs = SystemClock.elapsedRealtime()

        val activeAnnouncer = RunAnnouncer(this).also { it.start() }
        announcer = activeAnnouncer
        scope.launch { activeRunner.events.collect { runCatching { activeAnnouncer.onEvent(it) } } }

        startInForeground(buildNotification(activeRunner.state.value))
        if (freshStart) activeRunner.onStart(startedAtElapsedMs)
        scope.launch { saveSnapshotNow() }

        runJob?.cancel()
        runJob = scope.launch {
            launch {
                runCatching { locationSource.samples().collect { handleSample(it) } }
                    .onFailure { stopRun() }
            }
        }
        scope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                activeRunner.onTick(now)
                if (activeRunner.state.value.phase == RunnerPhase.STOPPED) {
                    stopRun()
                    break
                }
                notify(buildNotification(activeRunner.state.value))
                if (now - lastFlushMs >= FLUSH_INTERVAL_MS) {
                    lastFlushMs = now
                    flush()
                }
                delay(1000L)
            }
        }
    }

    private fun handleSample(sample: LocationSample) {
        val activeRunner = runner ?: return
        activeRunner.onSample(sample)
        trackBuffer += RunPersistence.toEntity(sample, sessionId, trackIdx++, activeRunner.state.value.stepIndex)
    }

    private suspend fun flush() {
        val snapshot = snapshot() ?: return
        val batch = trackBuffer.toList()
        trackBuffer.clear()
        runCatching {
            persistence?.appendTrackPoints(batch)
            persistence?.saveSnapshot(snapshot, System.currentTimeMillis())
        }
    }

    private suspend fun saveSnapshotNow() {
        val snapshot = snapshot() ?: return
        runCatching { persistence?.saveSnapshot(snapshot, System.currentTimeMillis()) }
    }

    private fun snapshot(): RunSnapshot? {
        val activeWorkout = workout ?: return null
        return RunSnapshot(
            sessionId = sessionId,
            workoutJson = WorkoutJson.encode(activeWorkout),
            startedAtElapsedMs = startedAtElapsedMs,
            startedAtEpochMs = startedAtEpochMs,
            zoneId = ZoneId.systemDefault().id,
        )
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopRun() {
        if (stopping) return
        stopping = true
        runner?.onStop(SystemClock.elapsedRealtime())
        runJob?.cancel()
        val finishedSession = sessionId
        ioScope.launch {
            runCatching {
                persistence?.appendTrackPoints(trackBuffer.toList())
                persistence?.clear(finishedSession)
            }
            trackBuffer.clear()
            withContext(Dispatchers.Main) {
                announcer?.shutdown()
                announcer = null
                scope.cancel()
                ServiceCompat.stopForeground(this@RunForegroundService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        ioScope.cancel()
        announcer?.shutdown()
        announcer = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    private fun buildNotification(state: RunnerState): Notification {
        val segmentLabel = state.segment?.label ?: getString(R.string.app_name)
        val content = "${Formatters.duration(state.movingMs)}  ·  ${Formatters.distanceKm(state.totalDistanceM)}  ·  " +
            Formatters.pace(state.paceLiveSecPerKm)
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, RunForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(segmentLabel)
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(0, getString(R.string.action_stop_run), stopIntent)
            .build()
    }

    private fun notify(notification: Notification) {
        ContextCompat.getSystemService(this, NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_run),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun freeRun() = Workout(
        id = "free",
        name = "Lari Bebas",
        items = listOf(Segment("Lari Bebas", SegmentKind.RUN, Limit.Open, targetPaceSecPerKm = null)),
    )

    companion object {
        private const val CHANNEL_ID = "run_ongoing"
        private const val NOTIFICATION_ID = 1
        private const val WAKE_LOCK_TAG = "RunCoach:run"
        private const val FLUSH_INTERVAL_MS = 5000L
        const val ACTION_STOP = "com.daniel.runcoach.action.STOP_RUN"
        const val ACTION_RESUME = "com.daniel.runcoach.action.RESUME_RUN"
        const val EXTRA_WORKOUT = "workout_json"
        const val EXTRA_SESSION_ID = "session_id"

        fun start(context: Context, workout: Workout) {
            val intent = Intent(context, RunForegroundService::class.java)
                .putExtra(EXTRA_WORKOUT, WorkoutJson.encode(workout))
            ContextCompat.startForegroundService(context, intent)
        }

        fun resume(context: Context, sessionId: String) {
            val intent = Intent(context, RunForegroundService::class.java)
                .setAction(ACTION_RESUME)
                .putExtra(EXTRA_SESSION_ID, sessionId)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, RunForegroundService::class.java).setAction(ACTION_STOP))
        }
    }
}
