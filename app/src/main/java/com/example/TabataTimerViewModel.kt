package com.example

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkoutPhase {
    IDLE,
    PREPARE,
    WORK,
    REST,
    CYCLE_REST,
    FINISHED
}

data class TimerSettings(
    val prepareTimeSeconds: Int = 10,
    val workTimeSeconds: Int = 20,
    val restTimeSeconds: Int = 10,
    val totalRounds: Int = 8,
    val totalCycles: Int = 1,
    val cycleRestSeconds: Int = 30,
    val isSoundEnabled: Boolean = true
)

data class WorkoutState(
    val settings: TimerSettings = TimerSettings(),
    val currentPhase: WorkoutPhase = WorkoutPhase.IDLE,
    val currentRound: Int = 1,
    val currentCycle: Int = 1,
    val secondsRemaining: Int = 0,
    val phaseDuration: Int = 0,
    val actualSecondsElapsed: Int = 0,
    val isTimerRunning: Boolean = false,
    val totalWorkoutDurationSeconds: Int = 0
)

class TabataTimerViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("tabata_preferences", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    init {
        // Initialize ToneGenerator for clean synthetic coaching sounds
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            toneGenerator = null
        }
        
        loadSettingsFromPreferences()
    }

    private fun loadSettingsFromPreferences() {
        val prep = sharedPrefs.getInt("prep_time", 10)
        val work = sharedPrefs.getInt("work_time", 20)
        val rest = sharedPrefs.getInt("rest_time", 10)
        val rounds = sharedPrefs.getInt("rounds", 8)
        val cycles = sharedPrefs.getInt("cycles", 1)
        val cycleRest = sharedPrefs.getInt("cycle_rest", 30)
        val sound = sharedPrefs.getBoolean("sound", true)

        val settings = TimerSettings(
            prepareTimeSeconds = prep,
            workTimeSeconds = work,
            restTimeSeconds = rest,
            totalRounds = rounds,
            totalCycles = cycles,
            cycleRestSeconds = cycleRest,
            isSoundEnabled = sound
        )
        
        _state.update {
            it.copy(
                settings = settings,
                totalWorkoutDurationSeconds = calculateTotalWorkoutDuration(settings)
            )
        }
    }

    private fun saveSettingsToPreferences(settings: TimerSettings) {
        sharedPrefs.edit().apply {
            putInt("prep_time", settings.prepareTimeSeconds)
            putInt("work_time", settings.workTimeSeconds)
            putInt("rest_time", settings.restTimeSeconds)
            putInt("rounds", settings.totalRounds)
            putInt("cycles", settings.totalCycles)
            putInt("cycle_rest", settings.cycleRestSeconds)
            putBoolean("sound", settings.isSoundEnabled)
            apply()
        }
    }

    // Mathematical breakdown of the dynamic workout duration
    private fun calculateTotalWorkoutDuration(s: TimerSettings): Int {
        var duration = 0
        
        // Initial warmup prepare phase (if configured)
        if (s.prepareTimeSeconds > 0) {
            duration += s.prepareTimeSeconds
        }
        
        // Cycles and Rounds
        for (c in 1..s.totalCycles) {
            for (r in 1..s.totalRounds) {
                // Work phase
                duration += s.workTimeSeconds
                
                // Rest phase
                if (r < s.totalRounds) {
                    duration += s.restTimeSeconds
                } else {
                    // Last round of the current cycle: transitions to cycle rest if there is another cycle
                    if (c < s.totalCycles) {
                        duration += s.cycleRestSeconds
                    }
                }
            }
        }
        return duration
    }

    fun applyPreset(work: Int, rest: Int, rounds: Int, cycles: Int = 1, cycleRest: Int = 30) {
        if (_state.value.currentPhase != WorkoutPhase.IDLE && _state.value.currentPhase != WorkoutPhase.FINISHED) {
            return // Prevent adjustments during active workout
        }
        
        _state.update {
            val updatedSettings = it.settings.copy(
                workTimeSeconds = work,
                restTimeSeconds = rest,
                totalRounds = rounds,
                totalCycles = cycles,
                cycleRestSeconds = cycleRest
            )
            saveSettingsToPreferences(updatedSettings)
            it.copy(
                settings = updatedSettings,
                totalWorkoutDurationSeconds = calculateTotalWorkoutDuration(updatedSettings),
                currentPhase = WorkoutPhase.IDLE,
                actualSecondsElapsed = 0
            )
        }
    }

    // Functions to modify individual preset settings
    fun updatePrepareTime(seconds: Int) {
        updateSetting { it.copy(prepareTimeSeconds = seconds.coerceIn(0, 300)) }
    }

    fun updateWorkTime(seconds: Int) {
        updateSetting { it.copy(workTimeSeconds = seconds.coerceIn(3, 600)) }
    }

    fun updateRestTime(seconds: Int) {
        updateSetting { it.copy(restTimeSeconds = seconds.coerceIn(3, 600)) }
    }

    fun updateRounds(count: Int) {
        updateSetting { it.copy(totalRounds = count.coerceIn(1, 99)) }
    }

    fun updateCycles(count: Int) {
        updateSetting { it.copy(totalCycles = count.coerceIn(1, 20)) }
    }

    fun updateCycleRestTime(seconds: Int) {
        updateSetting { it.copy(cycleRestSeconds = seconds.coerceIn(5, 600)) }
    }

    fun toggleSound() {
        _state.update {
            val updatedSettings = it.settings.copy(isSoundEnabled = !it.settings.isSoundEnabled)
            saveSettingsToPreferences(updatedSettings)
            it.copy(settings = updatedSettings)
        }
    }

    private inline fun updateSetting(transform: (TimerSettings) -> TimerSettings) {
        if (_state.value.currentPhase != WorkoutPhase.IDLE && _state.value.currentPhase != WorkoutPhase.FINISHED) {
            return
        }
        _state.update {
            val updatedSettings = transform(it.settings)
            saveSettingsToPreferences(updatedSettings)
            it.copy(
                settings = updatedSettings,
                totalWorkoutDurationSeconds = calculateTotalWorkoutDuration(updatedSettings),
                currentPhase = WorkoutPhase.IDLE,
                actualSecondsElapsed = 0
            )
        }
    }

    fun startTimer() {
        if (_state.value.isTimerRunning) return

        _state.update { it.copy(isTimerRunning = true) }

        // If starting fresh from IDLE or FINISHED
        if (_state.value.currentPhase == WorkoutPhase.IDLE || _state.value.currentPhase == WorkoutPhase.FINISHED) {
            val startPhase = if (_state.value.settings.prepareTimeSeconds > 0) {
                WorkoutPhase.PREPARE
            } else {
                WorkoutPhase.WORK
            }
            val initialSecondsRemaining = if (startPhase == WorkoutPhase.PREPARE) {
                _state.value.settings.prepareTimeSeconds
            } else {
                _state.value.settings.workTimeSeconds
            }

            _state.update {
                it.copy(
                    currentPhase = startPhase,
                    currentRound = 1,
                    currentCycle = 1,
                    secondsRemaining = initialSecondsRemaining,
                    phaseDuration = initialSecondsRemaining,
                    actualSecondsElapsed = 0
                )
            }
            // Trigger start beep
            playTone(ToneGenerator.TONE_PROP_BEEP2, 1)
        }

        launchTimerJob()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _state.update { it.copy(isTimerRunning = false) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _state.update {
            it.copy(
                currentPhase = WorkoutPhase.IDLE,
                currentRound = 1,
                currentCycle = 1,
                secondsRemaining = 0,
                phaseDuration = 0,
                actualSecondsElapsed = 0,
                isTimerRunning = false
            )
        }
    }

    private fun launchTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                tickOneSecond()
            }
        }
    }

    private fun tickOneSecond() {
        val currentStateValue = _state.value
        val remaining = currentStateValue.secondsRemaining - 1
        val elapsedTotal = currentStateValue.actualSecondsElapsed + 1

        if (remaining > 0) {
            // Beep at 3, 2, 1 for nice coaching UX
            if (remaining in 1..3) {
                playTone(ToneGenerator.TONE_PROP_BEEP, 1)
            }

            _state.update {
                it.copy(
                    secondsRemaining = remaining,
                    actualSecondsElapsed = elapsedTotal.coerceAtMost(it.totalWorkoutDurationSeconds)
                )
            }
        } else {
            // Transitions when current phase is complete!
            playTone(ToneGenerator.TONE_PROP_BEEP2, 2) // Dual beep for state transition
            transitionToNextPhase(elapsedTotal)
        }
    }

    private fun transitionToNextPhase(elapsedThusFar: Int) {
        val s = _state.value.settings
        val currentPhase = _state.value.currentPhase
        val currentRound = _state.value.currentRound
        val currentCycle = _state.value.currentCycle

        var nextPhase = WorkoutPhase.FINISHED
        var nextRound = currentRound
        var nextCycle = currentCycle
        var nextDurationSeconds = 0

        when (currentPhase) {
            WorkoutPhase.PREPARE -> {
                // Prepare finishes -> Always goes to WORK Round 1
                nextPhase = WorkoutPhase.WORK
                nextDurationSeconds = s.workTimeSeconds
                nextRound = 1
                nextCycle = 1
            }

            WorkoutPhase.WORK -> {
                if (currentRound < s.totalRounds) {
                    // Rest time inside active cycle
                    nextPhase = WorkoutPhase.REST
                    nextDurationSeconds = s.restTimeSeconds
                } else {
                    // Final round of current cycle
                    if (currentCycle < s.totalCycles) {
                        nextPhase = WorkoutPhase.CYCLE_REST
                        nextDurationSeconds = s.cycleRestSeconds
                    } else {
                        // Finished workout!
                        nextPhase = WorkoutPhase.FINISHED
                        nextDurationSeconds = 0
                        timerJob?.cancel()
                    }
                }
            }

            WorkoutPhase.REST -> {
                // Rest finishes -> Go to next WORK round
                nextPhase = WorkoutPhase.WORK
                nextDurationSeconds = s.workTimeSeconds
                nextRound = currentRound + 1
            }

            WorkoutPhase.CYCLE_REST -> {
                // Cycle rest finishes -> Go to round 1 of next cycle WORK
                nextPhase = WorkoutPhase.WORK
                nextDurationSeconds = s.workTimeSeconds
                nextRound = 1
                nextCycle = currentCycle + 1
            }

            WorkoutPhase.FINISHED, WorkoutPhase.IDLE -> {
                nextPhase = WorkoutPhase.FINISHED
                nextDurationSeconds = 0
            }
        }

        _state.update {
            it.copy(
                currentPhase = nextPhase,
                currentRound = nextRound,
                currentCycle = nextCycle,
                secondsRemaining = nextDurationSeconds,
                phaseDuration = nextDurationSeconds,
                actualSecondsElapsed = elapsedThusFar.coerceAtMost(it.totalWorkoutDurationSeconds),
                isTimerRunning = nextPhase != WorkoutPhase.FINISHED
            )
        }

        if (nextPhase == WorkoutPhase.FINISHED) {
            // Victory high-pitched beep
            playTone(ToneGenerator.TONE_CDMA_PIP, 5)
        }
    }

    private fun playTone(soundType: Int, repetitions: Int) {
        if (!_state.value.settings.isSoundEnabled || toneGenerator == null) return
        viewModelScope.launch {
            try {
                for (i in 1..repetitions) {
                    toneGenerator?.startTone(soundType, 120)
                    delay(180)
                }
            } catch (e: Exception) {
                // Ignore audio disruptions gracefully
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}
