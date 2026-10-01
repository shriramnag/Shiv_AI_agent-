package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ShivaiState
import com.example.ui.ShivaiViewModel
import com.example.ui.components.HolographicVoiceVisualizer
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

data class QuickActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val action: () -> Unit
)

@Composable
fun HomeScreen(
    viewModel: ShivaiViewModel,
    onNavigateToCallingMode: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToStudio: () -> Unit = {},
    onNavigateToNotes: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val networkAvailable by viewModel.networkAvailable.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val isVoiceRecognizing by viewModel.isVoiceRecognizing.collectAsState()
    val speechLiveRms by viewModel.speechLiveRms.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()

    val effectiveAmplitude = if (isVoiceRecognizing) speechLiveRms else amplitude

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.sendVoiceCommand(spokenText)
                onNavigateToChat()
            }
        }
    }

    val quickActions = listOf(
        QuickActionItem("AI Studio", "Vision, Video, Music", Icons.Default.AutoAwesome, NeonCyan) {
            onNavigateToStudio()
        },
        QuickActionItem("Voice Call", "Continuous Gemini Live", Icons.Default.Mic, NeonPurple) {
            onNavigateToCallingMode()
        },
        QuickActionItem("Read Screen", "Analyze active window", Icons.Default.Visibility, NeonCyan) {
            viewModel.sendTextMessage("Shivai, inspect current screen content and summarize visible UI elements.")
            onNavigateToChat()
        },
        QuickActionItem("Quick Note", "Create personal note", Icons.AutoMirrored.Filled.NoteAdd, NeonGold) {
            onNavigateToNotes()
        },
        QuickActionItem("Recall Memory", "View long-term facts", Icons.Default.Psychology, NeonGreen) {
            onNavigateToMemory()
        },
        QuickActionItem("Device Controls", "Torch, vibrate, audio", Icons.Default.Smartphone, NeonCyan) {
            viewModel.sendTextMessage("Shivai, show device control actions.")
            onNavigateToChat()
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Futuristic Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Column {
                    Text(
                        text = "SHIVAI",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "GEMINI 3.8 LIVE BRAIN",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, CircleShape)
                    .testTag("home_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Settings",
                    tint = TextPrimary
                )
            }
        }

        // Connection / Telemetry Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryBadge(
                label = if (networkAvailable) "ONLINE" else "OFFLINE",
                active = networkAvailable,
                color = if (networkAvailable) NeonGreen else NeonRed
            )
            TelemetryBadge(
                label = "WAKE: ${viewModel.settingsPrefs.wakePhrase.uppercase()}",
                active = viewModel.settingsPrefs.wakeWordEnabled,
                color = NeonCyan
            )
            TelemetryBadge(
                label = "MODE: ${viewModel.settingsPrefs.personality}",
                active = true,
                color = NeonGold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accessibility Service Notification Banner (if disabled)
        if (!isAccessibilityEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurfaceVariant)
                    .border(1.dp, NeonGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = NeonGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Screen Assistant",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Tap to enable Shivai in Accessibility Settings for screen reading & UI interaction.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Central Hologram Orb & Realtime Voice Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.5.dp,
                    if (isVoiceRecognizing) NeonRed.copy(alpha = 0.8f) else NeonCyan.copy(alpha = 0.3f),
                    RoundedCornerShape(24.dp)
                )
                .clickable {
                    if (isVoiceRecognizing) {
                        viewModel.stopRealtimeSpeech()
                    } else {
                        viewModel.startRealtimeSpeech(preferHindi = true)
                    }
                },
            colors = CardDefaults.cardColors(containerColor = CyberCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HolographicVoiceVisualizer(
                    amplitude = effectiveAmplitude,
                    state = if (isVoiceRecognizing) ShivaiState.LISTENING else state,
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isVoiceRecognizing) "LISTENING TO VOICE..." else statusText,
                    color = if (isVoiceRecognizing) NeonRed else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (liveTranscript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "“$liveTranscript”",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isVoiceRecognizing) "Speak your command now... tap to stop" else "Tap below or on Orb to start real-time speech input",
                    color = if (isVoiceRecognizing) NeonGold else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (isVoiceRecognizing) {
                                viewModel.stopRealtimeSpeech()
                            } else {
                                viewModel.startRealtimeSpeech(preferHindi = true)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVoiceRecognizing) NeonRed else NeonCyan
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_quick_voice_btn")
                    ) {
                        Icon(
                            imageVector = if (isVoiceRecognizing) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isVoiceRecognizing) "STOP LISTENING" else "REALTIME VOICE",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onNavigateToCallingMode,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .testTag("home_calling_mode_btn")
                    ) {
                        Text(
                            "LIVE CALL",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Actions Section
        Text(
            text = "QUICK ACTIONS",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(quickActions) { action ->
                QuickActionCard(action)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System Telemetry & Statistics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "MEMORIES",
                value = "${memories.size} Items",
                color = NeonGreen,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToMemory
            )
            StatCard(
                title = "NOTES",
                value = "${notes.size} Saved",
                color = NeonGold,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToNotes
            )
            StatCard(
                title = "TOOLS",
                value = "7 Active",
                color = NeonPurple,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToChat
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sample Voice Prompts Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "VOICE COMMAND SUGGESTIONS",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                PromptItem("• \"Shivai, open YouTube and search for Android tutorials\"")
                PromptItem("• \"Shivai, read what's on my screen right now\"")
                PromptItem("• \"Shivai, create a note: Buy groceries tomorrow\"")
                PromptItem("• \"Shivai, remember that my favorite color is cyber teal\"")
                PromptItem("• \"Shivai, turn on the flashlight and vibrate\"")
                PromptItem("• \"Shivai, stop\" (Instant barge-in)")
            }
        }
    }
}

@Composable
private fun QuickActionCard(item: QuickActionItem) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .clickable { item.action() },
        colors = CardDefaults.cardColors(containerColor = CyberCard)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(item.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = item.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = item.subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CyberCard)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun TelemetryBadge(label: String, active: Boolean, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurface)
            .border(0.8.dp, CyberBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (active) color else TextTertiary)
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PromptItem(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 3.dp)
    )
}
