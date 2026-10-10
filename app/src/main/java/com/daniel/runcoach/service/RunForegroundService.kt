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
import com.daniel.runcoach.engine.model.Limit
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

/**
 * Foreground service (type `location`) that owns the [WorkoutRunner] and the [LocationSource] while a
 * workout runs, so tracking continues with the screen off (plan P1.3). It holds a partial wake lock and
 * keeps an ongoing notification (time, distance, pace, segment) fresh every second. It stops only when the
 * user stops the run (via the notification action or an explicit intent).
 */
class RunForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var runner: WorkoutRunner? = null
    private lateinit var locationSource: LocationSource
    private var wakeLock: PowerManager.WakeLock? = null
    private var runJob: Job? = null
    private var announcer: RunAnnouncer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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
        }
        val workoutJson = intent?.getStringExtra(EXTRA_WORKOUT)
        val workout = workoutJson?.let { runCatching { WorkoutJson.decode(it) }.getOrNull() } ?: freeRun()
        startRun(workout)
        return START_NOT_STICKY
    }

    private fun startRun(workout: Workout) {
        val activeRunner = WorkoutRunner(workout)
        runner = activeRunner
        locationSource = LocationSourceFactory.create(this)

        val activeAnnouncer = RunAnnouncer(this).also { it.start() }
        announcer = activeAnnouncer
        scope.launch { activeRunner.events.collect { runCatching { activeAnnouncer.onEvent(it) } } }

        startInForeground(buildNotification(activeRunner.state.value))
        activeRunner.onStart(SystemClock.elapsedRealtime())

        runJob?.cancel()
        runJob = scope.launch {
            launch {
                runCatching { locationSource.samples().collect { activeRunner.onSample(it) } }
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
                delay(1000L)
            }
        }
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopRun() {
        runner?.onStop(SystemClock.elapsedRealtime())
        runJob?.cancel()
        announcer?.shutdown()
        announcer = null
        scope.cancel()
        notify(buildNotification(runner?.state?.value ?: RunnerState(phase = RunnerPhase.STOPPED)))
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
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
        const val ACTION_STOP = "com.daniel.runcoach.action.STOP_RUN"
        const val EXTRA_WORKOUT = "workout_json"

        fun start(context: Context, workout: Workout) {
            val intent = Intent(context, RunForegroundService::class.java)
                .putExtra(EXTRA_WORKOUT, WorkoutJson.encode(workout))
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, RunForegroundService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
