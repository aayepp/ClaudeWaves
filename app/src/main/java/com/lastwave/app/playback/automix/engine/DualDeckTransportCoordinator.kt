package com.lastwave.app.playback.automix.engine

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lastwave.app.playback.NativeAudioEngine
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.playback.automix.analysis.AcousticSignatureExtractor
import com.lastwave.app.playback.automix.api.AutoMixController
import com.lastwave.app.playback.automix.api.AutoMixPreferences
import com.lastwave.app.playback.automix.api.AutoMixSettings
import com.lastwave.app.playback.automix.api.BlendPhase
import com.lastwave.app.playback.automix.api.DeckBlendTelemetry
import com.lastwave.app.playback.automix.api.TrackChromeSummary
import com.lastwave.app.playback.automix.api.TransitionDurationMode
import com.lastwave.app.playback.automix.api.TransitionStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class DualDeckTransportCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AutoMixPreferences,
    private val signatureExtractor: AcousticSignatureExtractor,
) : AutoMixController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    var baseVolume: Float = 1.0f
        private set

    fun setBaseVolume(vol: Float) {
        baseVolume = vol.coerceIn(0f, 1f)
        if (!blendLoopActive) {
            currentPrimaryPlayer?.volume = baseVolume
        }
    }

    private val _telemetry = MutableStateFlow(DeckBlendTelemetry.IDLE)
    override val telemetry: StateFlow<DeckBlendTelemetry> = _telemetry.asStateFlow()

    private var currentSettings = AutoMixSettings()
    val isEnabled: Boolean get() = currentSettings.autoMixEnabled
    private var isHardwareBypassEngaged = false
    private var isJamRoomActive = false

    private var activeTrackKey: String? = null
    private var activeNextTrackKey: String? = null
    private var activeSpliceJob: Job? = null
    private var idleResetJob: Job? = null
    private var blendLoopActive = false
    private var deckSwapped = false
    private var savedOutgoingRepeatMode: Int? = null
    private var currentSplicePlan: SplicePlan? = null
    /** Outgoing duration the current plan was built for. */
    private var planDurationMs: Long = 0L
    /** An explicit "mix now" request: fires regardless of time left. */
    private var immediatePlan = false
    private var currentPrimaryPlayer: ExoPlayer? = null
    private var currentSecondaryPlayer: ExoPlayer? = null
    private var activeOutgoingEngine: NativeAudioEngine? = null
    private var activeIncomingEngine: NativeAudioEngine? = null
    /** User pause / audio-focus pause. A blend must hold still while it is set. */
    private var transportPaused = false

    /** True while a blend (both decks audible) is in progress. */
    val isBlending: Boolean get() = blendLoopActive

    /** Resolves the [NativeAudioEngine] bound to an ExoPlayer deck for 3-band spectral DSP. */
    var engineResolver: ((ExoPlayer) -> NativeAudioEngine?)? = null

    /** Resolves whether decoded PCM for a deck actually enters the native C++ DSP. */
    var nativeDspActiveResolver: ((ExoPlayer) -> Boolean)? = null

    /**
     * Pause / resume coming from the player transport. During a blend both
     * decks are paused and the blend clock stops, so the old song does not
     * keep fading out and the next song is not handed over while paused;
     * resume picks the blend up exactly where it stopped.
     */
    fun setTransportPaused(paused: Boolean) {
        if (transportPaused == paused) return
        transportPaused = paused
        if (!blendLoopActive) return
        runCatching {
            if (paused) {
                currentPrimaryPlayer?.pause()
                currentSecondaryPlayer?.pause()
            } else {
                currentPrimaryPlayer?.play()
                currentSecondaryPlayer?.play()
            }
        }
    }

    // Track reference callbacks into MusicPlayer
    var onPerformDeckSwap: ((outgoing: ExoPlayer, incoming: ExoPlayer) -> Unit)? = null
    var onTransitionComplete: (() -> Unit)? = null

    init {
        scope.launch {
            preferences.settings.collectLatest { settings ->
                currentSettings = settings
                if (!settings.autoMixEnabled) {
                    cancelTransition(revertToPrimary = true)
                }
            }
        }
    }

    fun onTrackProgressTick(
        primaryPlayer: ExoPlayer,
        secondaryPlayer: ExoPlayer?,
        currentTrack: PlayableTrack?,
        nextTrack: PlayableTrack?,
        positionMs: Long,
        durationMs: Long,
        nextTrackStreamUrl: String? = null,
        nextMediaId: String? = null,
        isRepeatOne: Boolean = false,
        isPodcast: Boolean = false,
        isSleepTimerEndOfTrack: Boolean = false,
        isAbLoopActive: Boolean = false,
    ) {
        if (!currentSettings.autoMixEnabled ||
            isHardwareBypassEngaged ||
            isJamRoomActive ||
            isRepeatOne ||
            isPodcast ||
            isSleepTimerEndOfTrack ||
            isAbLoopActive
        ) {
            if (blendLoopActive || _telemetry.value.phase != BlendPhase.IDLE) {
                cancelTransition(revertToPrimary = true)
            }
            return
        }

        // If a blend is actively running, do not interfere with the transition in progress
        if (blendLoopActive) {
            return
        }

        currentPrimaryPlayer = primaryPlayer
        currentSecondaryPlayer = secondaryPlayer

        if (currentTrack == null || nextTrack == null || durationMs <= 0L) return

        val trackKey = currentTrack.videoId ?: "${currentTrack.artist}|${currentTrack.title}"
        val nextKey = nextMediaId ?: nextTrack.videoId ?: "${nextTrack.artist}|${nextTrack.title}"
        if (activeTrackKey != trackKey || activeNextTrackKey != nextKey) {
            activeTrackKey = trackKey
            activeNextTrackKey = nextKey
            currentSplicePlan = null
            immediatePlan = false
            // Only reset telemetry if no blend is active or completing —
            // the deck swap changes the current track mid-blend, which must
            // not flicker the badge back to the quality label.
            val currentPhase = _telemetry.value.phase
            if (currentPhase != BlendPhase.BLENDING &&
                currentPhase != BlendPhase.LOOP_LOCKED &&
                currentPhase != BlendPhase.MIX_COMPLETED
            ) {
                _telemetry.value = DeckBlendTelemetry.IDLE
            }
        }

        // A plan built for a different length (the stream's real duration
        // arrived after the plan) would open the mix at the wrong moment.
        if (currentSplicePlan != null && !immediatePlan && abs(planDurationMs - durationMs) > 1_500L) {
            currentSplicePlan = null
            if (_telemetry.value.phase == BlendPhase.ARMED) {
                _telemetry.update { it.copy(phase = BlendPhase.PRE_ANALYZING) }
            }
        }

        val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)

        // 1. Look-ahead pre-analysis trigger: 25% progress or 90s remaining
        if (positionMs > durationMs * 0.25f || remainingMs < 90_000L) {
            if (_telemetry.value.phase == BlendPhase.IDLE) {
                _telemetry.update {
                    it.copy(
                        phase = BlendPhase.PRE_ANALYZING,
                        incomingTrackPreview = TrackChromeSummary(
                            title = nextTrack.title,
                            artist = nextTrack.artist,
                            artworkUrl = nextTrack.artworkUrl,
                            durationMs = nextTrack.durationMs ?: 0L,
                        ),
                    )
                }

                signatureExtractor.preAnalyzeTrack(nextTrack, nextTrackStreamUrl ?: nextTrack.playbackUrl) { sig ->
                    val phaseNow = _telemetry.value.phase
                    if (activeTrackKey != trackKey ||
                        activeNextTrackKey != nextKey ||
                        blendLoopActive ||
                        (phaseNow != BlendPhase.PRE_ANALYZING && phaseNow != BlendPhase.ARMED)
                    ) {
                        return@preAnalyzeTrack
                    }
                    val plan = SpliceTrajectorySynthesizer.synthesizePlan(
                        outgoingTrack = currentTrack,
                        outgoingSignature = null,
                        incomingTrack = nextTrack,
                        incomingSignature = sig,
                        settings = currentSettings,
                        outgoingDurationMs = durationMs,
                        incomingDurationMs = nextTrack.durationMs,
                    )
                    currentSplicePlan = plan
                    planDurationMs = durationMs

                    if (!plan.isBypass) {
                        _telemetry.update {
                            it.copy(
                                phase = BlendPhase.ARMED,
                                spliceStartPositionMs = plan.cueOutStartMs,
                                spliceDurationMs = plan.durationMs,
                                activeTransitionStyle = plan.transitionStyle,
                                harmonicKeyPair = sig.camelotKey.toString(),
                                syncedBpm = sig.beatGrid.bpm.value,
                            )
                        }
                    } else {
                        _telemetry.update { it.copy(phase = BlendPhase.ARMED) }
                    }
                }
            }
        }

        // 2. Automatic Fallback Plan (respects AUTO 12s or MANUAL 12s..30s)
        val rawFallbackMs = when (currentSettings.durationMode) {
            TransitionDurationMode.AUTO -> 12_000L
            TransitionDurationMode.MANUAL -> (currentSettings.targetDurationSeconds * 1000L).coerceIn(12_000L, 30_000L)
        }
        val fallbackDurationMs = minOf(rawFallbackMs, (durationMs * 0.40f).toLong()).coerceAtLeast(4_000L)
        if (remainingMs <= fallbackDurationMs + 1_000L) {
            val plan = currentSplicePlan
            if (plan == null || plan.isBypass) {
                val style = if (currentSettings.spectralFilterSweepsEnabled) {
                    TransitionStyle.SPECTRAL_SWEEP
                } else {
                    TransitionStyle.PERCEPTUAL_FADE
                }
                val safePlan = SplicePlan(
                    cueOutStartMs = maxOf(0L, durationMs - fallbackDurationMs),
                    cueInStartMs = 0L,
                    durationMs = fallbackDurationMs,
                    transitionStyle = style,
                    isBypass = false,
                )
                currentSplicePlan = safePlan
                planDurationMs = durationMs
                _telemetry.update {
                    it.copy(
                        phase = BlendPhase.ARMED,
                        spliceStartPositionMs = safePlan.cueOutStartMs,
                        spliceDurationMs = safePlan.durationMs,
                        activeTransitionStyle = safePlan.transitionStyle,
                    )
                }
            }
        }

        // 3. Trigger Splice at Splice Horizon (never while paused; verify deck 2 holds expected next item)
        val plan = currentSplicePlan
        if (!transportPaused && primaryPlayer.playWhenReady &&
            plan != null && !plan.isBypass &&
            (_telemetry.value.phase == BlendPhase.ARMED || _telemetry.value.phase == BlendPhase.PRE_ANALYZING)
        ) {
            val shouldTrigger = immediatePlan || (
                remainingMs <= plan.durationMs + 1_000L &&
                    (positionMs >= plan.cueOutStartMs || remainingMs <= plan.durationMs)
                )
            if (shouldTrigger && secondaryPlayer != null && secondaryPlayer !== primaryPlayer) {
                val matchesNext = nextMediaId == null || secondaryPlayer.currentMediaItem?.mediaId == nextMediaId
                val isSecondaryReady = secondaryPlayer.playbackState == Player.STATE_READY &&
                    secondaryPlayer.playerError == null &&
                    matchesNext
                val speed = primaryPlayer.playbackParameters.speed.coerceAtLeast(0.25f)
                val remainingWallMs = (remainingMs / speed).toLong()
                val enoughRunway = immediatePlan || remainingWallMs >= 1_500L
                if (isSecondaryReady && enoughRunway) {
                    val effectiveDurationMs = if (immediatePlan) {
                        plan.durationMs
                    } else {
                        minOf(plan.durationMs, maxOf(3_000L, remainingWallMs))
                    }
                    startBlendExecution(
                        primaryPlayer = primaryPlayer,
                        secondaryPlayer = secondaryPlayer,
                        plan = plan.copy(durationMs = effectiveDurationMs),
                    )
                }
            }
        }
    }

    private fun startBlendExecution(
        primaryPlayer: ExoPlayer,
        secondaryPlayer: ExoPlayer,
        plan: SplicePlan,
    ) {
        activeSpliceJob?.cancel()
        idleResetJob?.cancel()
        idleResetJob = null
        immediatePlan = false
        blendLoopActive = true
        deckSwapped = false
        currentPrimaryPlayer = primaryPlayer
        currentSecondaryPlayer = secondaryPlayer
        val durationMs = plan.durationMs.coerceAtLeast(1_000L)

        val outEngine = runCatching { engineResolver?.invoke(primaryPlayer) }.getOrNull()
        val inEngine = runCatching { engineResolver?.invoke(secondaryPlayer) }.getOrNull()
        activeOutgoingEngine = outEngine
        activeIncomingEngine = inEngine

        val useSpectralDsp = currentSettings.spectralFilterSweepsEnabled &&
            outEngine != null && inEngine != null &&
            outEngine.isAvailable && inEngine.isAvailable &&
            !outEngine.systemFlattened && !inEngine.systemFlattened &&
            nativeDspActiveResolver?.invoke(primaryPlayer) != false &&
            nativeDspActiveResolver?.invoke(secondaryPlayer) != false

        savedOutgoingRepeatMode = primaryPlayer.repeatMode
        primaryPlayer.repeatMode = Player.REPEAT_MODE_OFF

        _telemetry.update {
            it.copy(
                phase = if (plan.requiresPhraseLoop) BlendPhase.LOOP_LOCKED else BlendPhase.BLENDING,
                blendProgress = 0.0f,
                spliceDurationMs = durationMs,
                activeTransitionStyle = plan.transitionStyle,
                loopBarsActive = if (plan.requiresPhraseLoop) plan.loopBarsCount else null,
            )
        }

        secondaryPlayer.playbackParameters = primaryPlayer.playbackParameters

        // Avoid calling seekTo(0) when already parked near 0ms; seekTo flushes ExoPlayer buffers.
        val secPos = runCatching { secondaryPlayer.currentPosition }.getOrDefault(0L)
        val secBuf = runCatching { secondaryPlayer.bufferedPosition }.getOrDefault(0L)
        if (plan.cueInStartMs > 100L && plan.cueInStartMs <= secBuf) {
            secondaryPlayer.seekTo(plan.cueInStartMs)
        } else if (secPos > 500L && plan.cueInStartMs <= 100L) {
            secondaryPlayer.seekTo(0L)
        }

        if (useSpectralDsp) {
            inEngine?.setAutoMixBandGains(true, 0f, 0f, 0f)
            outEngine?.setAutoMixBandGains(true, 1f, 1f, 1f)
            primaryPlayer.volume = baseVolume
            secondaryPlayer.volume = baseVolume
        } else {
            outEngine?.setAutoMixBandGains(false, 1f, 1f, 1f)
            inEngine?.setAutoMixBandGains(false, 1f, 1f, 1f)
            secondaryPlayer.volume = 0.0f
            primaryPlayer.volume = baseVolume
        }

        secondaryPlayer.play()

        activeSpliceJob = scope.launch {
            val myJob = coroutineContext[Job]
            var elapsed = 0L
            var waitingBufferMs = 0L
            var lastTick = android.os.SystemClock.elapsedRealtime()
            var aborted = false

            try {
                while (isActive) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    val dt = (now - lastTick).coerceAtLeast(0L)
                    lastTick = now

                    if (secondaryPlayer.playerError != null) {
                        aborted = true
                        cancelTransition(revertToPrimary = true)
                        return@launch
                    }

                    if (transportPaused) {
                        delay(20L)
                        continue
                    }

                    if (secondaryPlayer.playbackState == Player.STATE_BUFFERING && elapsed == 0L && waitingBufferMs < 1_500L) {
                        waitingBufferMs += dt
                        delay(20L)
                        continue
                    }

                    elapsed += dt
                    var progress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0.0f, 1.0f)
                    if (!deckSwapped && primaryPlayer.playbackState == Player.STATE_ENDED) {
                        progress = progress.coerceAtLeast(0.65f)
                    } else if (deckSwapped && primaryPlayer.playbackState == Player.STATE_ENDED) {
                        progress = 1.0f
                    }

                    _telemetry.update { it.copy(blendProgress = progress) }

                    if (useSpectralDsp) {
                        applySpectralBandGains(
                            outEngine = outEngine,
                            inEngine = inEngine,
                            progress = progress,
                            primaryPlayer = primaryPlayer,
                            secondaryPlayer = secondaryPlayer,
                        )
                    } else {
                        val gainOut = cos(progress * PI * 0.5).toFloat().coerceIn(0.0f, 1.0f)
                        val gainIn = sin(progress * PI * 0.5).toFloat().coerceIn(0.0f, 1.0f)
                        val sum = gainIn + gainOut
                        val trim = if (sum <= 1f) 1f else 1f / sqrt(sum)
                        primaryPlayer.volume = (gainOut * trim * baseVolume).coerceIn(0f, 1f)
                        secondaryPlayer.volume = (gainIn * trim * baseVolume).coerceIn(0f, 1f)
                    }

                    // Dominant energy handoff point (65% through transition window):
                    if (progress >= 0.65f && !deckSwapped) {
                        deckSwapped = true
                        runCatching {
                            onPerformDeckSwap?.invoke(primaryPlayer, secondaryPlayer)
                        }.onFailure { e ->
                            android.util.Log.e("DualDeckCoordinator", "Error during deck swap", e)
                        }
                    }

                    if (progress >= 1.0f) break
                    delay(20L)
                }

                if (!aborted) {
                    if (!deckSwapped) {
                        deckSwapped = true
                        runCatching { onPerformDeckSwap?.invoke(primaryPlayer, secondaryPlayer) }
                    }
                    resetAutoMixEngines()
                    primaryPlayer.volume = 0.0f
                    secondaryPlayer.volume = baseVolume
                    runCatching { primaryPlayer.pause() }

                    blendLoopActive = false
                    deckSwapped = false
                    savedOutgoingRepeatMode = null
                    currentSplicePlan = null
                    currentPrimaryPlayer = null
                    currentSecondaryPlayer = null
                    if (activeSpliceJob === myJob) activeSpliceJob = null

                    _telemetry.update {
                        it.copy(
                            phase = BlendPhase.MIX_COMPLETED,
                            blendProgress = 1.0f,
                        )
                    }

                    runCatching { onTransitionComplete?.invoke() }

                    idleResetJob?.cancel()
                    idleResetJob = scope.launch {
                        delay(1_500L)
                        if (!blendLoopActive) {
                            _telemetry.update { DeckBlendTelemetry.IDLE }
                        }
                    }
                }
            } finally {
                if (activeSpliceJob === myJob) {
                    blendLoopActive = false
                    activeSpliceJob = null
                    resetAutoMixEngines()
                }
            }
        }
    }

    private fun applySpectralBandGains(
        outEngine: NativeAudioEngine?,
        inEngine: NativeAudioEngine?,
        progress: Float,
        primaryPlayer: ExoPlayer,
        secondaryPlayer: ExoPlayer,
    ) {
        val t = progress.coerceIn(0f, 1f)
        val halfPi = (PI * 0.5).toFloat()

        // 1. Sub-bass (< 120 Hz): tight DJ bass-swap across 0.42..0.58 so low ends never clash
        val outLow: Float
        val inLow: Float
        when {
            t < 0.42f -> {
                outLow = 1.0f
                inLow = 0.0f
            }
            t > 0.58f -> {
                outLow = 0.0f
                inLow = 1.0f
            }
            else -> {
                val subT = (t - 0.42f) / 0.16f
                outLow = cos(subT * halfPi).coerceIn(0f, 1f)
                inLow = sin(subT * halfPi).coerceIn(0f, 1f)
            }
        }

        // 2. Mid band (120 Hz .. 3.8 kHz): smooth equal-power contour across whole transition
        val outMid = cos(t * halfPi).coerceIn(0f, 1f)
        val inMid = sin(t * halfPi).coerceIn(0f, 1f)

        // 3. High band (> 3.8 kHz): incoming highs enter early (0..0.75) while outgoing tails out (0.20..1.0)
        val inHigh = sin(((t / 0.75f).coerceIn(0f, 1f)) * halfPi).coerceIn(0f, 1f)
        val outHigh = cos((((t - 0.20f) / 0.80f).coerceIn(0f, 1f)) * halfPi).coerceIn(0f, 1f)

        val headroomTrim = (0.88f + 0.12f * abs(2f * t - 1f)).coerceIn(0.88f, 1f)

        outEngine?.setAutoMixBandGains(
            true,
            (outLow * headroomTrim).coerceIn(0f, 1f),
            (outMid * headroomTrim).coerceIn(0f, 1f),
            (outHigh * headroomTrim).coerceIn(0f, 1f),
        )
        inEngine?.setAutoMixBandGains(
            true,
            (inLow * headroomTrim).coerceIn(0f, 1f),
            (inMid * headroomTrim).coerceIn(0f, 1f),
            (inHigh * headroomTrim).coerceIn(0f, 1f),
        )
        primaryPlayer.volume = baseVolume
        secondaryPlayer.volume = baseVolume
    }

    private fun resetAutoMixEngines() {
        runCatching { activeOutgoingEngine?.setAutoMixBandGains(false, 1f, 1f, 1f) }
        runCatching { activeIncomingEngine?.setAutoMixBandGains(false, 1f, 1f, 1f) }
        activeOutgoingEngine = null
        activeIncomingEngine = null
    }

    override fun cancelTransition(revertToPrimary: Boolean) {
        val job = activeSpliceJob
        activeSpliceJob = null
        job?.cancel()
        idleResetJob?.cancel()
        idleResetJob = null

        val wasSwapped = deckSwapped
        val savedRepeat = savedOutgoingRepeatMode
        val primary = currentPrimaryPlayer
        val secondary = currentSecondaryPlayer

        blendLoopActive = false
        deckSwapped = false
        savedOutgoingRepeatMode = null
        currentSplicePlan = null
        immediatePlan = false
        currentPrimaryPlayer = null
        currentSecondaryPlayer = null
        resetAutoMixEngines()
        _telemetry.update { DeckBlendTelemetry.IDLE }

        if (wasSwapped) {
            // Deck swap already promoted secondary as active player; stop outgoing primary cleanly.
            runCatching {
                secondary?.volume = baseVolume
                primary?.volume = 0.0f
                primary?.pause()
                onTransitionComplete?.invoke()
            }
        } else if (revertToPrimary) {
            runCatching {
                savedRepeat?.let { primary?.repeatMode = it }
                primary?.volume = baseVolume
                secondary?.volume = 0.0f
                secondary?.pause()
            }
        }
    }

    override fun forceCommitTransition() {
        val job = activeSpliceJob
        activeSpliceJob = null
        job?.cancel()
        idleResetJob?.cancel()
        idleResetJob = null

        val wasSwapped = deckSwapped
        val primary = currentPrimaryPlayer
        val secondary = currentSecondaryPlayer

        blendLoopActive = false
        deckSwapped = false
        savedOutgoingRepeatMode = null
        currentSplicePlan = null
        immediatePlan = false
        currentPrimaryPlayer = null
        currentSecondaryPlayer = null
        resetAutoMixEngines()

        if (primary != null && secondary != null) {
            runCatching {
                if (!wasSwapped) {
                    onPerformDeckSwap?.invoke(primary, secondary)
                }
                primary.volume = 0.0f
                secondary.volume = baseVolume
                primary.pause()
                onTransitionComplete?.invoke()
            }
        }
        _telemetry.update {
            it.copy(
                phase = BlendPhase.MIX_COMPLETED,
                blendProgress = 1.0f,
            )
        }
        idleResetJob = scope.launch {
            delay(1_500L)
            if (!blendLoopActive) {
                _telemetry.update { DeckBlendTelemetry.IDLE }
            }
        }
    }

    override fun triggerImmediateTransition(customDurationMs: Long?) {
        val duration = customDurationMs ?: (currentSettings.targetDurationSeconds * 1000L)
        val plan = SplicePlan(
            cueOutStartMs = 0L,
            cueInStartMs = 0L,
            durationMs = duration,
            transitionStyle = TransitionStyle.HARMONIC_BEATMIX,
        )
        currentSplicePlan = plan
        immediatePlan = true
    }

    override fun handleSeekEvent(targetPositionMs: Long) {
        if (!blendLoopActive) return
        if (deckSwapped) {
            // Active player is already incoming track; finish outgoing tail cleanly.
            forceCommitTransition()
            return
        }
        val plan = currentSplicePlan ?: return
        if (targetPositionMs >= plan.cueOutStartMs + (plan.durationMs * 0.65f)) {
            forceCommitTransition()
        } else {
            cancelTransition(revertToPrimary = true)
        }
    }

    override fun updateHardwareBypassState(isHardwareBypassActive: Boolean) {
        isHardwareBypassEngaged = isHardwareBypassActive
        if (isHardwareBypassActive) {
            cancelTransition(revertToPrimary = true)
        }
    }

    fun setJamRoomActive(active: Boolean) {
        isJamRoomActive = active
        if (active) {
            cancelTransition(revertToPrimary = true)
        }
    }
}
