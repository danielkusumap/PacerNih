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
import com.daniel.runcoach.data.AppSettings
import com.daniel.runcoach.data.CountdownStyleSetting
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.data.SettingsRepository
import com.daniel.runcoach.engine.alert.AlertEngine
import com.daniel.runcoach.engine.alert.CorrectionInput
import com.daniel.runcoach.engine.alert.CorrectionState
import com.daniel.runcoach.engine.metrics.PaceCalculator
import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.gps.DistanceTracker
import com.daniel.runcoach.engine.gps.GpsOutcome
import com.daniel.runcoach.engine.gps.SampleFilter
import com.daniel.runcoach.engine.naming.DefaultSessionNamer
import com.daniel.runcoach.engine.result.DistTimePoint
import com.daniel.runcoach.engine.result.SessionResultsCalculator
import com.daniel.runcoach.engine.result.SkippedSegment
import com.daniel.runcoach.engine.result.SessionResultsData
import com.daniel.runcoach.engine.voice.VoiceLanguage
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.LocationSample
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.model.WorkoutExpander
import com.daniel.runcoach.engine.model.WorkoutJson
import com.daniel.runcoach.engine.runner.RunnerPhase
import com.daniel.runcoach.engine.runner.RunnerState
import com.daniel.runcoach.engine.runner.WorkoutRunner
import com.daniel.runcoach.service.location.LocationSource
import com.daniel.runcoach.service.location.LocationSourceFactory
import com.daniel.runcoach.service.voice.RunAnnouncer
import com.daniel.runcoach.service.voice.CountdownStyle
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
import java.util.Locale
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
    private var alertEngine: AlertEngine? = null

    private var sessionId: String = ""
    private var workout: Workout? = null
    private var startedAtElapsedMs: Long = 0L
    private var startedAtEpochMs: Long = 0L
    private var stopping = false
    private var trackIdx = 0
    private var lastFlushMs = 0L
    private var startCountdownSec: Int? = null
    private var voiceLanguage: VoiceLanguage = VoiceLanguage.ID
    private var expandedSteps: List<Segment> = emptyList()
    private var appSettings: AppSettings = AppSettings()
    private var lastAccuracyM: Double? = null
    private val liveTrack = ArrayDeque<Pair<Double, Double>>()
    private val trackBuffer = mutableListOf<com.daniel.runcoach.data.TrackPointEntity>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        persistence = RunPersistence(this)
        scope.launch { SettingsRepository(this@RunForegroundService).settings.collect { appSettings = it } }
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

            ACTION_CANCEL_COUNTDOWN -> {
                runner?.onCancelCountdown()
                return START_NOT_STICKY
            }

            ACTION_PAUSE -> {
                runner?.onPause(SystemClock.elapsedRealtime())
                return START_NOT_STICKY
            }

            ACTION_RESUME_RUN -> {
                runner?.onResume(SystemClock.elapsedRealtime())
                return START_NOT_STICKY
            }

            ACTION_SKIP -> {
                runner?.onSkipSegment(SystemClock.elapsedRealtime())
                return START_NOT_STICKY
            }

            ACTION_EXTEND -> {
                runner?.onExtendSegment(
                    deltaMeters = intent.getDoubleExtra(EXTRA_EXTEND_METERS, 0.0),
                    deltaSeconds = intent.getIntExtra(EXTRA_EXTEND_SECONDS, 0),
                )
                return START_NOT_STICKY
            }
        }
        val workoutJson = intent?.getStringExtra(EXTRA_WORKOUT)
        val workout = workoutJson?.let { runCatching { WorkoutJson.decode(it) }.getOrNull() } ?: freeRun()
        startCountdownSec = intent?.getIntExtra(EXTRA_START_COUNTDOWN, -1)?.takeIf { it >= 0 }
        voiceLanguage = when (intent?.getStringExtra(EXTRA_VOICE_LANG)) {
            "en" -> VoiceLanguage.EN
            else -> VoiceLanguage.ID
        }
        startNewRun(workout)
        return START_NOT_STICKY
    }

    private fun startNewRun(workout: Workout) {
        FinishedSessionHolder.clear()
        this.workout = workout
        sessionId = UUID.randomUUID().toString()
        startedAtElapsedMs = SystemClock.elapsedRealtime()
        startedAtEpochMs = System.currentTimeMillis()
        liveTrack.clear()
        val config = EngineConfig(
            startCountdownSec = startCountdownSec ?: appSettings.startCountdownSec,
            autoPauseEnabled = appSettings.autoPause,
            cueDistanceM = appSettings.cueDistanceM,
        )
        launchRun(WorkoutRunner(workout, config, calibrationFactor = appSettings.calibrationFactor), freshStart = true)
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
            val replayed = WorkoutRunner(workout, calibrationFactor = appSettings.calibrationFactor)
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
        alertEngine = AlertEngine()
        expandedSteps = workout?.let { w ->
            runCatching { WorkoutExpander.expandFinite(w.items) }
                .getOrElse { WorkoutExpander.lazySteps(w.items).take(80).toList() }
        } ?: emptyList()

        val activeAnnouncer = RunAnnouncer(
            this,
            language = voiceLanguage,
            countdownStyle = if (appSettings.countdownStyle == CountdownStyleSetting.VOICE) CountdownStyle.VOICE else CountdownStyle.BEEP,
            vibrateEnabled = appSettings.vibrate,
        ).also { it.start() }
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
                updateUi(activeRunner, now)
                if (activeRunner.state.value.phase == RunnerPhase.STOPPED) {
                    stopRun()
                    break
                }
                notify(buildNotification(activeRunner.state.value))
                if (now - lastFlushMs >= FLUSH_INTERVAL_MS) {
                    lastFlushMs = now
                    flush()
                }
                delay(250L)
            }
        }
    }

    /** Feeds the alert engine and publishes the run state to the UI (P1.7). */
    private fun updateUi(activeRunner: WorkoutRunner, nowMs: Long) {
        val engine = alertEngine ?: return
        val state = activeRunner.state.value
        val input = correctionInput(state)
        val event = input?.let { engine.evaluate(it, nowMs) }
        if (event != null) runCatching { announcer?.onEvent(event) }
        val required = if (engine.state() == CorrectionState.TOO_SLOW) input?.requiredSecPerKm else null
        RunStateHolder.update(
            RunUiState(
                phase = state.phase,
                workoutName = workout?.name ?: "",
                state = state,
                correction = engine.state(),
                requiredPaceSecPerKm = required,
                keepScreenOn = false,
                steps = expandedSteps,
                trackPoints = liveTrack.toList(),
                accuracyM = lastAccuracyM,
            ),
        )
    }

    private fun correctionInput(state: RunnerState): CorrectionInput? {
        val segment = state.segment ?: return null
        val target = segment.targetPaceSecPerKm
        val timeBased = segment.limit is Limit.Duration
        val limitM = (segment.limit as? Limit.Distance)?.meters?.toDouble() ?: 0.0
        val limitSec = (segment.limit as? Limit.Duration)?.seconds?.toDouble() ?: 0.0
        val doneM = state.segmentDistanceM
        val scope = PaceCalculator.scope(limitM, doneM, segment.perKmCheck && target != null)
        val doneInScope = (doneM - scope.startM).coerceAtLeast(0.0)
        val remainingScopeM = (scope.distanceM - doneInScope).coerceAtLeast(0.0)
        val elapsedScopeSec = state.segmentMovingMs / 1000.0
        val paceLive = state.paceLiveSecPerKm
        val projected = paceLive?.let {
            PaceCalculator.projectedPaceSecPerKm(it, elapsedScopeSec, scope.distanceM, remainingScopeM)
        }
        val remainingSegmentSec = when {
            timeBased -> state.segmentRemainingMs / 1000.0
            paceLive != null && paceLive > 0.0 -> (state.segmentRemainingM / 1000.0) * paceLive
            else -> 0.0
        }
        val required = target?.let {
            PaceCalculator.requiredPaceSecPerKm(it.toDouble(), elapsedScopeSec, scope.distanceM, remainingScopeM)
        }
        return CorrectionInput(
            segmentTargetSecPerKm = target,
            fastMarginSec = segment.fastMarginSec,
            segmentOpen = segment.limit is Limit.Open,
            timeBased = timeBased,
            segmentLengthM = limitM,
            segmentLengthSec = limitSec,
            segmentDistanceM = doneM,
            segmentMovingSec = elapsedScopeSec,
            scopeDistM = scope.distanceM,
            remainingSegmentSec = remainingSegmentSec,
            paceLiveSecPerKm = paceLive,
            projectedSecPerKm = projected,
            requiredSecPerKm = required,
            running = state.phase == RunnerPhase.RUNNING,
            gpsLost = state.gpsLost,
            targetBound = segment.targetBound,
            targetPace2SecPerKm = segment.targetPace2SecPerKm,
        )
    }

    private fun handleSample(sample: LocationSample) {
        val activeRunner = runner ?: return
        lastAccuracyM = sample.accuracyM
        activeRunner.onSample(sample)
        trackBuffer += RunPersistence.toEntity(sample, sessionId, trackIdx++, activeRunner.state.value.stepIndex)
        liveTrack.addLast(sample.lat to sample.lon)
        if (liveTrack.size > 400) liveTrack.removeFirst()
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
        val stateAtStop = runner?.state?.value
        val skippedAtStop = runner?.skippedSegments() ?: emptyMap()
        runner?.onStop(SystemClock.elapsedRealtime())
        RunStateHolder.reset()
        alertEngine = null
        runJob?.cancel()
        val finishedSession = sessionId
        val activeWorkout = workout
        // Make the id available to S06 immediately, before the async finalize writes the draft.
        FinishedSessionHolder.sessionId = finishedSession
        ioScope.launch {
            runCatching {
                persistence?.appendTrackPoints(trackBuffer.toList())
                trackBuffer.clear()
                finalizeSession(finishedSession, activeWorkout, stateAtStop, skippedAtStop)
            }
            withContext(Dispatchers.Main) {
                announcer?.shutdown()
                announcer = null
                scope.cancel()
                ServiceCompat.stopForeground(this@RunForegroundService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    /**
     * Prepares the finished session (results computed from the persisted track points) but does **not** save it:
     * S06 decides. Track points stay in the DB; only the active snapshot is dropped so Beranda no longer offers
     * resume.
     */
    private suspend fun finalizeSession(
        id: String,
        activeWorkout: Workout?,
        state: RunnerState?,
        skipped: Map<Int, SkippedSegment>,
    ) {
        val store = persistence ?: return
        val points = buildCurve(id)
        val stoppedFrom = if (state?.planCompleted == true) null else state?.stepIndex?.takeIf { it > 0 }
        val results = activeWorkout?.let { SessionResultsCalculator.compute(it, points, skipped, stoppedFrom) }
            ?: SessionResultsData(emptyList(), emptyList(), emptyList())
        val distanceM = state?.totalDistanceM ?: points.lastOrNull()?.cumulativeDistanceM ?: 0.0
        val movingMs = state?.movingMs ?: points.lastOrNull()?.elapsedMs ?: 0L
        val zone = ZoneId.systemDefault().id
        val language = if (Locale.getDefault().language.startsWith("in") || Locale.getDefault().language == "id") {
            VoiceLanguage.ID
        } else {
            VoiceLanguage.EN
        }
        val now = System.currentTimeMillis()
        val entity = SessionEntity(
            id = id,
            templateId = null,
            templateNameSnapshot = activeWorkout?.name ?: "",
            workoutSnapshotJson = activeWorkout?.let { WorkoutJson.encode(it) } ?: "",
            name = DefaultSessionNamer.name(startedAtEpochMs, zone, language),
            nameIsCustom = false,
            note = null,
            rpe = null,
            excludeFromRecords = false,
            startedAtEpochMs = startedAtEpochMs,
            zoneId = zone,
            endedAtEpochMs = now,
            elapsedMs = state?.elapsedMs ?: 0L,
            movingMs = movingMs,
            pausedMs = state?.pausedMs ?: 0L,
            totalDistanceM = distanceM,
            avgPaceSecPerKm = if (distanceM > 0) movingMs / distanceM else null,
            segPass = results.segments.count { it.status == "PASS" },
            segFail = results.segments.count { it.status == "FAIL" },
            segBorderline = results.segments.count { it.status == "BORDERLINE" },
            segNoTarget = results.segments.count { it.status == "NO_TARGET" },
            planCompleted = state?.planCompleted ?: false,
            goalReached = state?.goalReached ?: false,
            distanceCalibrationFactorUsed = appSettings.calibrationFactor,
            droppedSampleCount = state?.droppedSampleCount ?: 0,
            updatedAt = now,
        )
        runCatching { SessionRepository(this@RunForegroundService).saveDraft(entity, results) }
        FinishedSessionHolder.sessionId = id
        runCatching { store.clearActiveSnapshot(id) }
    }

    /** Rebuilds the cumulative distance/moving-time curve from the persisted track points. */
    private suspend fun buildCurve(id: String): List<DistTimePoint> {
        val trackPoints = persistence?.loadTrackPoints(id).orEmpty()
        val filter = SampleFilter()
        val tracker = DistanceTracker()
        val curve = mutableListOf<DistTimePoint>()
        for (point in trackPoints) {
            val filtered = filter.accept(RunPersistence.toSample(point))
            if (filtered.outcome == GpsOutcome.REJECTED) continue
            val movingMs = point.timeMs - startedAtElapsedMs
            tracker.add(movingMs, filtered)
            curve += DistTimePoint(tracker.totalDistanceM, movingMs)
        }
        return curve
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
        const val ACTION_RESUME = "com.daniel.runcoach.action.RESUME_SESSION"
        const val ACTION_CANCEL_COUNTDOWN = "com.daniel.runcoach.action.CANCEL_COUNTDOWN"
        const val ACTION_PAUSE = "com.daniel.runcoach.action.PAUSE"
        const val ACTION_RESUME_RUN = "com.daniel.runcoach.action.UNPAUSE"
        const val ACTION_SKIP = "com.daniel.runcoach.action.SKIP"
        const val ACTION_EXTEND = "com.daniel.runcoach.action.EXTEND"
        const val EXTRA_EXTEND_METERS = "extend_meters"
        const val EXTRA_EXTEND_SECONDS = "extend_seconds"
        const val EXTRA_WORKOUT = "workout_json"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_START_COUNTDOWN = "start_countdown_sec"
        const val EXTRA_VOICE_LANG = "voice_lang"

        fun start(context: Context, workout: Workout, countdownSec: Int? = null, language: VoiceLanguage = VoiceLanguage.ID) {
            val intent = Intent(context, RunForegroundService::class.java)
                .putExtra(EXTRA_WORKOUT, WorkoutJson.encode(workout))
                .putExtra(EXTRA_VOICE_LANG, if (language == VoiceLanguage.EN) "en" else "id")
            if (countdownSec != null) intent.putExtra(EXTRA_START_COUNTDOWN, countdownSec)
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

        fun cancelCountdown(context: Context) {
            context.startService(Intent(context, RunForegroundService::class.java).setAction(ACTION_CANCEL_COUNTDOWN))
        }

        fun pause(context: Context) {
            context.startService(Intent(context, RunForegroundService::class.java).setAction(ACTION_PAUSE))
        }

        fun resumeRun(context: Context) {
            context.startService(Intent(context, RunForegroundService::class.java).setAction(ACTION_RESUME_RUN))
        }

        fun skip(context: Context) {
            context.startService(Intent(context, RunForegroundService::class.java).setAction(ACTION_SKIP))
        }

        fun extend(context: Context, meters: Double, seconds: Int) {
            context.startService(
                Intent(context, RunForegroundService::class.java)
                    .setAction(ACTION_EXTEND)
                    .putExtra(EXTRA_EXTEND_METERS, meters)
                    .putExtra(EXTRA_EXTEND_SECONDS, seconds),
            )
        }
    }
}
