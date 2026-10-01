package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ShivaiState
import com.example.ui.ShivaiViewModel
import com.example.ui.components.HolographicVoiceVisualizer
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun CallingModeScreen(
    viewModel: ShivaiViewModel,
    onEndCall: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isSpeakerMuted by viewModel.isSpeakerMuted.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val transcript by viewModel.liveTranscript.collectAsState()
    val networkAvailable by viewModel.networkAvailable.collectAsState()

    val stateBadgeColor = when (state) {
        ShivaiState.LISTENING -> NeonCyan
        ShivaiState.THINKING -> NeonPurple
        ShivaiState.SPEAKING -> NeonGreen
        ShivaiState.INTERRUPTED -> NeonRed
        ShivaiState.CONNECTING -> NeonPurple
        ShivaiState.CONNECTED -> NeonCyan
        ShivaiState.ERROR -> NeonRed
        ShivaiState.STANDBY -> TextSecondary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CyberBlack, Color(0xFF070B14), CyberBlack)
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Status Bar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live connection badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurface)
                            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (networkAvailable) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Connection Status",
                            tint = if (networkAvailable) NeonGreen else NeonRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (networkAvailable) "GEMINI LIVE 3.8" else "OFFLINE",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Mic / Speaker telemetry
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(
                            label = if (isMicMuted) "MIC MUTED" else "MIC LIVE",
                            active = !isMicMuted,
                            activeColor = NeonCyan
                        )
                        StatusChip(
                            label = if (isSpeakerMuted) "AUDIO OFF" else "SPEAKER ON",
                            active = !isSpeakerMuted,
                            activeColor = NeonGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big State Display
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(stateBadgeColor.copy(alpha = 0.12f))
                        .border(1.dp, stateBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = stateBadgeColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = state.name,
                            color = stateBadgeColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = statusText,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Central Holographic Waveform Visualizer
            Box(
                modifier = Modifier
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                HolographicVoiceVisualizer(
                    amplitude = amplitude,
                    state = state,
                    modifier = Modifier.size(260.dp)
                )
            }

            // Realtime Conversation Activity / Transcript stream
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberCard)
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "LIVE ACTIVITY",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (transcript.isNotBlank()) transcript else "Continuous listening active. Speak freely or say \"Shivai, stop\" to interrupt.",
                    color = if (transcript.isNotBlank()) TextPrimary else TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Control Actions Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute Toggle
                IconButton(
                    onClick = { viewModel.toggleMicMute() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isMicMuted) NeonRed.copy(alpha = 0.2f) else CyberSurface)
                        .border(1.dp, if (isMicMuted) NeonRed else CyberBorder, CircleShape)
                        .testTag("calling_mic_toggle")
                ) {
                    Icon(
                        imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle Microphone",
                        tint = if (isMicMuted) NeonRed else NeonCyan
                    )
                }

                // Stop / Interrupt Button (Barge-In)
                IconButton(
                    onClick = { viewModel.interruptSpeaking() },
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.2f))
                        .border(1.5.dp, NeonPurple, CircleShape)
                        .testTag("calling_interrupt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Shivai Speaking",
                        tint = NeonPurple,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Speaker Mute Toggle
                IconButton(
                    onClick = { viewModel.toggleSpeakerMute() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isSpeakerMuted) NeonRed.copy(alpha = 0.2f) else CyberSurface)
                        .border(1.dp, if (isSpeakerMuted) NeonRed else CyberBorder, CircleShape)
                        .testTag("calling_speaker_toggle")
                ) {
                    Icon(
                        imageVector = if (isSpeakerMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Toggle Speaker",
                        tint = if (isSpeakerMuted) NeonRed else NeonGreen
                    )
                }

                // End Calling Mode Button
                FloatingActionButton(
                    onClick = {
                        viewModel.stopCallingMode()
                        onEndCall()
                    },
                    containerColor = NeonRed,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("calling_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Calling Mode",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    active: Boolean,
    activeColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurface)
            .border(0.8.dp, if (active) activeColor.copy(alpha = 0.5f) else CyberBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else TextTertiary)
        )
        Text(
            text = label,
            color = if (active) TextPrimary else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
