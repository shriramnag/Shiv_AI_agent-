package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SettingsPreferences
import com.example.ui.ShivaiViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: ShivaiViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()

    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showLongPressMenu by remember { mutableStateOf(false) }
    var isTestingKey by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    fun pasteAndSaveApiKey() {
        val clipText = clipboardManager.getText()?.text
        if (!clipText.isNullOrBlank()) {
            val cleanKey = SettingsPreferences.sanitizeApiKey(clipText)
            if (cleanKey.isNotBlank()) {
                apiKeyInput = cleanKey
                viewModel.saveApiKey(cleanKey)
                Toast.makeText(context, "✅ API Key पेस्ट और सेव हो गई!", Toast.LENGTH_SHORT).show()
                // Auto test key in background
                isTestingKey = true
                testResult = null
                scope.launch {
                    val res = viewModel.testApiKey()
                    isTestingKey = false
                    testResult = res
                }
            } else {
                Toast.makeText(context, "क्लिपबोर्ड में कोई मान्य API Key नहीं मिली!", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "क्लिपबोर्ड खाली है! पहले Google AI Studio से API Key कॉपी करें।", Toast.LENGTH_LONG).show()
        }
    }

    var liveModel by remember { mutableStateOf(viewModel.settingsPrefs.liveModel) }
    var textModel by remember { mutableStateOf(viewModel.settingsPrefs.textModel) }
    var selectedVoice by remember { mutableStateOf(viewModel.settingsPrefs.voiceName) }
    var personality by remember { mutableStateOf(viewModel.settingsPrefs.personality) }

    var wakeWordEnabled by remember { mutableStateOf(viewModel.settingsPrefs.wakeWordEnabled) }
    var wakePhrase by remember { mutableStateOf(viewModel.settingsPrefs.wakePhrase) }
    var wakeSensitivity by remember { mutableStateOf(viewModel.settingsPrefs.wakeSensitivity) }

    var bargeInEnabled by remember { mutableStateOf(viewModel.settingsPrefs.bargeInEnabled) }
    var requireConfirmation by remember { mutableStateOf(viewModel.settingsPrefs.requireConfirmation) }

    LaunchedEffect(Unit) {
        viewModel.refreshAccessibilityStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberSurface)
                .border(1.dp, CyberBorder)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("settings_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Shivai Core Settings",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. AI & GEMINI API SECTION
            SettingsSectionHeader(title = "GEMINI INTELLIGENCE LAYER", icon = Icons.Default.Key, color = NeonCyan)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Gemini API Key",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Current Status: ${viewModel.settingsPrefs.getMaskedApiKey()}",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // One-Tap Quick Paste Button
                    Button(
                        onClick = { pasteAndSaveApiKey() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .testTag("settings_paste_clipboard_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste from Clipboard",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "PASTE FROM CLIPBOARD (क्लिपबोर्ड से पेस्ट करें)",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Interactive Input Box with Long-Press Gesture Detection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        showLongPressMenu = true
                                    }
                                )
                            }
                    ) {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                            },
                            placeholder = { Text("Long-press here or tap Paste (यहाँ दबाकर रखें)", color = TextSecondary, fontSize = 12.sp) },
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { pasteAndSaveApiKey() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { isKeyVisible = !isKeyVisible },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isKeyVisible) "Hide Key" else "Show Key",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (apiKeyInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = { apiKeyInput = "" },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_api_key_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = CyberBlack.copy(alpha = 0.5f),
                                unfocusedContainerColor = CyberBlack.copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )

                        // Long-Press Floating Context Menu
                        DropdownMenu(
                            expanded = showLongPressMenu,
                            onDismissRequest = { showLongPressMenu = false },
                            modifier = Modifier
                                .background(CyberCard)
                                .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "PASTE (पेस्ट करें)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showLongPressMenu = false
                                    pasteAndSaveApiKey()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            if (isKeyVisible) "Hide Key (छुपाएं)" else "Show Key (दिखाएं)",
                                            color = TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showLongPressMenu = false
                                    isKeyVisible = !isKeyVisible
                                }
                            )

                            if (apiKeyInput.isNotBlank()) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = null,
                                                tint = NeonRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                "Clear Input (साफ़ करें)",
                                                color = NeonRed,
                                                fontSize = 13.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        showLongPressMenu = false
                                        apiKeyInput = ""
                                    }
                                )
                            }
                        }
                    }

                    // Helper & Format Tip
                    if (apiKeyInput.isNotBlank()) {
                        if (apiKeyInput.startsWith("AIzaSy")) {
                            Text(
                                text = "✓ Gemini API Key प्रारूप सही है (${apiKeyInput.length} अक्षर)",
                                color = NeonGreen,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "⚠ ध्यान दें: सामान्यतः Gemini API Key 'AIzaSy' से शुरू होती है।",
                                color = NeonGold,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Text(
                            text = "💡 सुझाव: बॉक्स पर थोड़ी देर दबाकर रखें (Long Press) या 'PASTE' बटन दबाएं।",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    val cleaned = apiKeyInput.trim()
                                        .removeSurrounding("\"")
                                        .removeSurrounding("'")
                                        .trim()
                                    viewModel.saveApiKey(cleaned)
                                    apiKeyInput = ""
                                    testResult = null
                                    Toast.makeText(context, "API Key saved securely.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter or paste an API Key first.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_save_api_key_btn")
                        ) {
                            Text("Save Key", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                isTestingKey = true
                                testResult = null
                                scope.launch {
                                    val res = viewModel.testApiKey()
                                    isTestingKey = false
                                    testResult = res
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_test_api_key_btn")
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                            } else {
                                Text("Test Key", color = NeonCyan)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.deleteApiKey()
                                testResult = null
                                Toast.makeText(context, "API Key removed.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Delete", color = NeonRed)
                        }
                    }

                    testResult?.let { (success, msg) ->
                        Text(
                            text = msg,
                            color = if (success) NeonGreen else NeonRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Configurable Models",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = liveModel,
                        onValueChange = {
                            liveModel = it
                            viewModel.settingsPrefs.liveModel = it
                        },
                        label = { Text("Live Voice Model") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = textModel,
                        onValueChange = {
                            textModel = it
                            viewModel.settingsPrefs.textModel = it
                        },
                        label = { Text("Text & Tools Model") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. VOICE & GEMINI LIVE SETTINGS
            SettingsSectionHeader(title = "VOICE & GEMINI LIVE", icon = Icons.Default.RecordVoiceOver, color = NeonPurple)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Live Prebuilt Voice",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val voices = listOf("Aoede", "Puck", "Charon", "Fenrir", "Kore")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        voices.forEach { voice ->
                            FilterChip(
                                selected = selectedVoice == voice,
                                onClick = {
                                    selectedVoice = voice
                                    viewModel.settingsPrefs.voiceName = voice
                                },
                                label = { Text(voice, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonPurple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Barge-in / Natural Interruption",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Interrupts Shivai speaking as soon as your voice is detected.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = bargeInEnabled,
                            onCheckedChange = {
                                bargeInEnabled = it
                                viewModel.settingsPrefs.bargeInEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonPurple
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.speakTextResponse("नमस्ते! मैं शिवाय हूँ। आपकी सहायता के लिए तैयार हूँ। Hello! I am Shivai, your AI assistant.")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TEST NATURAL SPEECH (HINDI & ENGLISH)", color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. WAKE WORD SECTION
            SettingsSectionHeader(title = "WAKE WORD DETECTION", icon = Icons.Default.Speed, color = NeonGold)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wake Word Detection",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Passively listens for wake phrase to activate Shivai hands-free.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = {
                                wakeWordEnabled = it
                                viewModel.updateWakeWordSettings(it, wakePhrase, wakeSensitivity)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonGold
                            )
                        )
                    }

                    Text(
                        text = "Wake Phrase",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val phrases = listOf("Shivai", "Hey Shivai")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        phrases.forEach { phrase ->
                            FilterChip(
                                selected = wakePhrase == phrase,
                                onClick = {
                                    wakePhrase = phrase
                                    viewModel.updateWakeWordSettings(wakeWordEnabled, phrase, wakeSensitivity)
                                },
                                label = { Text(phrase) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGold,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Text(
                        text = "Sensitivity",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val sensitivities = listOf("LOW", "MEDIUM", "HIGH")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        sensitivities.forEach { sens ->
                            FilterChip(
                                selected = wakeSensitivity == sens,
                                onClick = {
                                    wakeSensitivity = sens
                                    viewModel.updateWakeWordSettings(wakeWordEnabled, wakePhrase, sens)
                                },
                                label = { Text(sens) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGold,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            // 4. PERSONALITY SETTINGS
            SettingsSectionHeader(title = "PERSONALITY & BEHAVIOR", icon = Icons.Default.Psychology, color = NeonGreen)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val personalities = listOf(
                        Triple("NORMAL", "Balanced & High-Tech", "Efficient, poised, futuristic assistant."),
                        Triple("SERIOUS", "Tactical & Direct", "Concise, formal, no filler words."),
                        Triple("COMPANION", "Empathetic & Warm", "Friendly, supportive, conversational.")
                    )

                    personalities.forEach { (mode, label, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (personality == mode) NeonGreen.copy(alpha = 0.12f) else CyberSurface)
                                .border(1.dp, if (personality == mode) NeonGreen else CyberBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    personality = mode
                                    viewModel.settingsPrefs.personality = mode
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, if (personality == mode) NeonGreen else TextSecondary, CircleShape)
                                    .padding(3.dp)
                            ) {
                                if (personality == mode) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(NeonGreen)
                                    )
                                }
                            }
                            Column {
                                Text(text = "$mode ($label)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = desc, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 5. AUTOMATION & ACCESSIBILITY
            SettingsSectionHeader(title = "SYSTEM AUTOMATION & SECURITY", icon = Icons.Default.Security, color = NeonRed)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Screen Assistant Service",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isAccessibilityEnabled) "Status: ACTIVE & CONNECTED" else "Status: DISABLED in Android Settings",
                                color = if (isAccessibilityEnabled) NeonGreen else NeonGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityEnabled) CyberSurface else NeonGold
                            )
                        ) {
                            Text(
                                text = if (isAccessibilityEnabled) "Config" else "Enable",
                                color = if (isAccessibilityEnabled) TextPrimary else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sensitive Action Confirmation",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Requires explicit tap confirmation before calls, SMS, WhatsApp, or memory wipes.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = requireConfirmation,
                            onCheckedChange = {
                                requireConfirmation = it
                                viewModel.settingsPrefs.requireConfirmation = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonRed
                            )
                        )
                    }

                    // Floating Cyber Bubble Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating Cyber Bubble",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Shows a floating futuristic orb above any app for quick voice access.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        val overlayActive by viewModel.floatingOverlayActive.collectAsState()
                        Switch(
                            checked = overlayActive,
                            onCheckedChange = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                                    context.startActivity(intent)
                                } else {
                                    viewModel.toggleFloatingOverlay(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }

                    // Voiceprint & Biometric Lock Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voiceprint & Biometric Lock",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Requires your biometric authentication for sensitive actions and private vault.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        val bioActive by viewModel.biometricSecurityActive.collectAsState()
                        Switch(
                            checked = bioActive,
                            onCheckedChange = { viewModel.toggleBiometricSecurity() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonGreen
                            )
                        )
                    }

                    // Home Assistant / IoT endpoint configuration
                    var haUrl by remember { mutableStateOf(viewModel.settingsPrefs.homeAssistantUrl) }
                    OutlinedTextField(
                        value = haUrl,
                        onValueChange = {
                            haUrl = it
                            viewModel.settingsPrefs.homeAssistantUrl = it
                        },
                        label = { Text("Home Assistant URL (Local IP / Domain)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}
