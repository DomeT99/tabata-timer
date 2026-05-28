package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SportAccent
import com.example.ui.theme.SportBorderNeutral
import com.example.ui.theme.SportCardBg
import com.example.ui.theme.SportCardBgPressed
import com.example.ui.theme.SportCycleRest
import com.example.ui.theme.SportDarkBg
import com.example.ui.theme.SportFinished
import com.example.ui.theme.SportGrayText
import com.example.ui.theme.SportPrepare
import com.example.ui.theme.SportRest
import com.example.ui.theme.SportWhite
import com.example.ui.theme.SportWork

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SportDarkBg),
                    containerColor = SportDarkBg
                ) { innerPadding ->
                    TabataTimerApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TabataTimerApp(
    modifier: Modifier = Modifier,
    viewModel: TabataTimerViewModel = viewModel()
) {
    val workoutState by viewModel.state.collectAsState()
    val isTimerActive = workoutState.currentPhase != WorkoutPhase.IDLE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SportDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. TOOLBAR HEADER
        HeaderToolbar(
            isSoundEnabled = workoutState.settings.isSoundEnabled,
            onToggleSound = { viewModel.toggleSound() }
        )

        // MAIN WORKOUT SCREEN VIEWS (DUAL MODES)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = isTimerActive,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                },
                label = "MainViewTransition"
            ) { active ->
                if (active) {
                    // GYM HUD MODE - Giant high-contrast graphics focused entirely on active training progress
                    GymHudScreen(
                        state = workoutState,
                        onReset = { viewModel.resetTimer() }
                    )
                } else {
                    // CONFIG MODE - Rich layout containing intervals configuration sliders & presets rows
                    ConfigScreen(
                        state = workoutState,
                        viewModel = viewModel
                    )
                }
            }
        }

        // BOTTOM PRIMARY ACTION BAR
        PrimaryControlsBar(
            state = workoutState,
            onStart = { viewModel.startTimer() },
            onPause = { viewModel.pauseTimer() },
            onReset = { viewModel.resetTimer() }
        )

        // DECORATIVE PREMIUM BOTTOM NAVIGATION
        DecorativeBottomNav()
    }
}

@Composable
fun HeaderToolbar(
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Tabata Pro",
                color = SportWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "HIGH INTENSITY",
                color = SportGrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SportCardBg)
                .clickable(onClick = onToggleSound)
                .testTag("sound_toggle_button"),
            contentAlignment = Alignment.Center
        ) {
            VolumeIcon(
                isEnabled = isSoundEnabled,
                tint = if (isSoundEnabled) SportAccent else SportGrayText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ConfigScreen(
    state: WorkoutState,
    viewModel: TabataTimerViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Large display total estimated workout time
        Text(
            text = "TOTAL SESSION DURATION",
            color = SportGrayText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = formatMMSS(state.totalWorkoutDurationSeconds),
            color = SportWhite,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Quick Preset Tags row
        PresetSelectorsRow(onSelectPreset = { work, rest, rounds, cycles, cycleRest ->
            viewModel.applyPreset(work, rest, rounds, cycles, cycleRest)
        })

        Spacer(modifier = Modifier.height(16.dp))

        // Interval Controllers Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = SportBorderNeutral,
                    shape = RoundedCornerShape(24.dp)
                )
                .background(SportCardBg, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Pre-warmup prepare / Rounds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AdjustableSettingItem(
                            title = "WARMUP PREP",
                            value = "${state.settings.prepareTimeSeconds}s",
                            color = SportPrepare,
                            onDecrease = { viewModel.updatePrepareTime(state.settings.prepareTimeSeconds - 1) },
                            onIncrease = { viewModel.updatePrepareTime(state.settings.prepareTimeSeconds + 1) },
                            decreaseTag = "prep_dec",
                            increaseTag = "prep_inc"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdjustableSettingItem(
                            title = "ROUNDS / CYCLE",
                            value = "${state.settings.totalRounds}",
                            color = SportAccent,
                            onDecrease = { viewModel.updateRounds(state.settings.totalRounds - 1) },
                            onIncrease = { viewModel.updateRounds(state.settings.totalRounds + 1) },
                            decreaseTag = "rounds_dec",
                            increaseTag = "rounds_inc"
                        )
                    }
                }

                // Section 2: Work & Rest Intermissions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AdjustableSettingItem(
                            title = "WORK INTERVAL",
                            value = "${state.settings.workTimeSeconds}s",
                            color = SportWork,
                            onDecrease = { viewModel.updateWorkTime(state.settings.workTimeSeconds - 5) },
                            onIncrease = { viewModel.updateWorkTime(state.settings.workTimeSeconds + 5) },
                            decreaseTag = "work_dec",
                            increaseTag = "work_inc"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdjustableSettingItem(
                            title = "REST INTERVAL",
                            value = "${state.settings.restTimeSeconds}s",
                            color = SportRest,
                            onDecrease = { viewModel.updateRestTime(state.settings.restTimeSeconds - 1) },
                            onIncrease = { viewModel.updateRestTime(state.settings.restTimeSeconds + 1) },
                            decreaseTag = "rest_dec",
                            increaseTag = "rest_inc"
                        )
                    }
                }

                // Section 3: Advanced Multiple Cycles & Cycle Rests
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AdjustableSettingItem(
                            title = "TOTAL CYCLES",
                            value = "${state.settings.totalCycles}",
                            color = SportCycleRest,
                            onDecrease = { viewModel.updateCycles(state.settings.totalCycles - 1) },
                            onIncrease = { viewModel.updateCycles(state.settings.totalCycles + 1) },
                            decreaseTag = "cycles_dec",
                            increaseTag = "cycles_inc"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdjustableSettingItem(
                            title = "CYCLE RECOVER",
                            value = "${state.settings.cycleRestSeconds}s",
                            color = SportCycleRest,
                            onDecrease = { viewModel.updateCycleRestTime(state.settings.cycleRestSeconds - 5) },
                            onIncrease = { viewModel.updateCycleRestTime(state.settings.cycleRestSeconds + 5) },
                            decreaseTag = "cycle_rest_dec",
                            increaseTag = "cycle_rest_inc"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetSelectorsRow(
    onSelectPreset: (work: Int, rest: Int, rounds: Int, cycles: Int, cycleRest: Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val presets = listOf(
            PresetData("TABATA", 20, 10, 8, 1, 30, SportWork),
            PresetData("CARDIO BOOST", 30, 15, 10, 1, 30, SportRest),
            PresetData("HIIT SHRED", 45, 15, 8, 3, 60, SportCycleRest)
        )

        presets.forEach { preset ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = SportBorderNeutral,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        onSelectPreset(
                            preset.work,
                            preset.rest,
                            preset.rounds,
                            preset.cycles,
                            preset.cycleRest
                        )
                    }
                    .background(SportCardBg)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = preset.name,
                        color = SportWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${preset.work}s/${preset.rest}s",
                        color = preset.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

data class PresetData(
    val name: String,
    val work: Int,
    val rest: Int,
    val rounds: Int,
    val cycles: Int,
    val cycleRest: Int,
    val accentColor: Color
)

@Composable
fun AdjustableSettingItem(
    title: String,
    value: String,
    color: Color,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseTag: String,
    increaseTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = SportBorderNeutral,
                shape = RoundedCornerShape(24.dp)
            )
            .background(SportCardBg, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = SportGrayText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            color = color,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Standard 48dp Touch Area buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SportCardBgPressed)
                    .clickable(role = Role.Button, onClick = onDecrease)
                    .testTag(decreaseTag),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp, 2.dp)
                        .background(SportWhite)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SportCardBgPressed)
                    .clickable(role = Role.Button, onClick = onIncrease)
                    .testTag(increaseTag),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase Value",
                    tint = SportWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun GymHudScreen(
    state: WorkoutState,
    onReset: () -> Unit
) {
    val phaseDetails = getPhaseDetails(state.currentPhase)
    val phaseProgressRatio = if (state.phaseDuration > 0) {
        state.secondsRemaining.toFloat() / state.phaseDuration.toFloat()
    } else {
        0f
    }

    val animatedProgressFraction by animateFloatAsState(
        targetValue = phaseProgressRatio,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "CircularProgressProgress"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text(
                text = phaseDetails.title,
                color = phaseDetails.color,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = phaseDetails.tagline,
                color = SportGrayText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                phaseDetails.color.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthValue = 8.dp.toPx()
                drawCircle(
                    color = SportBorderNeutral,
                    style = Stroke(width = strokeWidthValue)
                )

                drawArc(
                    color = phaseDetails.color,
                    startAngle = -270f,
                    sweepAngle = animatedProgressFraction * 360f,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidthValue,
                        cap = StrokeCap.Round
                    )
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val pulseScale = if (state.secondsRemaining in 1..3 && state.currentPhase != WorkoutPhase.FINISHED) {
                    1.15f
                } else {
                    1.0f
                }

                Text(
                    text = if (state.currentPhase == WorkoutPhase.FINISHED) "🏆" else formatMMSS(state.secondsRemaining),
                    color = SportWhite,
                    fontSize = if (state.currentPhase == WorkoutPhase.FINISHED) 44.sp else 54.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFeatureSettings = "tnum"
                    ),
                    modifier = Modifier.scale(pulseScale),
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = SportBorderNeutral,
                    shape = RoundedCornerShape(24.dp)
                )
                .background(SportCardBg, RoundedCornerShape(24.dp))
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ROUNDS",
                    color = SportGrayText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${state.currentRound} / ${state.settings.totalRounds}",
                    color = SportWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(SportBorderNeutral)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CYCLES",
                    color = SportGrayText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${state.currentCycle} / ${state.settings.totalCycles}",
                    color = SportWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL PROGRESS",
                    color = SportGrayText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${formatMMSS(state.actualSecondsElapsed)} / ${formatMMSS(state.totalWorkoutDurationSeconds)}",
                    color = SportWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val totalProgressFraction = if (state.totalWorkoutDurationSeconds > 0) {
                state.actualSecondsElapsed.toFloat() / state.totalWorkoutDurationSeconds.toFloat()
            } else {
                0f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(SportBorderNeutral)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(totalProgressFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SportWork, SportRest)
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun PrimaryControlsBar(
    state: WorkoutState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isTimerRunning = state.isTimerRunning
        val isTimerActive = state.currentPhase != WorkoutPhase.IDLE

        AnimatedVisibility(
            visible = isTimerActive,
            enter = fadeIn(animationSpec = spring()),
            exit = fadeOut(animationSpec = spring())
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SportCardBgPressed)
                    .clickable(onClick = onReset)
                    .testTag("reset_timer_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Restart Workout",
                    tint = SportWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(SportAccent)
                .clickable(onClick = {
                    if (isTimerRunning) onPause() else onStart()
                })
                .testTag("play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTimerRunning) {
                    PauseIcon(
                        color = Color(0xFF0A0A0A),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "PAUSE",
                        color = Color(0xFF0A0A0A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Workout",
                        tint = Color(0xFF0A0A0A),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "START",
                        color = Color(0xFF0A0A0A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DecorativeBottomNav() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SportAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val r = size.minDimension / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    drawCircle(color = SportAccent, radius = r, style = Stroke(width = 2.dp.toPx()))
                    drawLine(color = SportAccent, start = center, end = androidx.compose.ui.geometry.Offset(center.x, center.y - r * 0.6f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = SportAccent, start = center, end = androidx.compose.ui.geometry.Offset(center.x + r * 0.4f, center.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            Text(
                text = "TIMER",
                color = SportAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        
    }
}

// ------------------- AUXILIARY DRAWN VECTOR COMPOSABLES (REPLACEMENT FOR COMPLEX ICON PACKS) -------------------

@Composable
fun PauseIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 6.dp, height = 22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(width = 6.dp, height = 22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }
}

@Composable
fun VolumeIcon(
    isEnabled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        
        // Speaker body shape
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.15f, h * 0.35f)
            lineTo(w * 0.4f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.15f)
            lineTo(w * 0.65f, h * 0.85f)
            lineTo(w * 0.4f, h * 0.65f)
            lineTo(w * 0.15f, h * 0.65f)
            close()
        }
        drawPath(path, tint)

        if (isEnabled) {
            // Drawn audio wave lines
            drawArc(
                color = tint,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.25f),
                size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.5f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        } else {
            // Mute cross layout
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.38f),
                end = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.62f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.38f),
                end = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.62f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

// ------------------- AUXILIARY STATE LOGIC UTILS -------------------

data class PhaseUiDetails(
    val title: String,
    val tagline: String,
    val color: Color
)

fun getPhaseDetails(phase: WorkoutPhase): PhaseUiDetails {
    return when (phase) {
        WorkoutPhase.PREPARE -> PhaseUiDetails(
            title = "GET READY",
            tagline = "Warm up and prepare to go",
            color = SportPrepare
        )

        WorkoutPhase.WORK -> PhaseUiDetails(
            title = "GO GO GO!",
            tagline = "Push your absolute hardest!",
            color = SportWork
        )

        WorkoutPhase.REST -> PhaseUiDetails(
            title = "REST UP",
            tagline = "Catch your breath and recover",
            color = SportRest
        )

        WorkoutPhase.CYCLE_REST -> PhaseUiDetails(
            title = "CYCLE RECOVER",
            tagline = "Stay focused for the next cycle",
            color = SportCycleRest
        )

        WorkoutPhase.FINISHED -> PhaseUiDetails(
            title = "FINISHED!",
            tagline = "Workout complete, exceptional job!",
            color = SportFinished
        )

        WorkoutPhase.IDLE -> PhaseUiDetails(
            title = "PREPARED",
            tagline = "Ready to start",
            color = SportWhite
        )
    }
}

fun formatMMSS(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
