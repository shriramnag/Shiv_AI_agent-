package com.example.ui.screens.studio

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun AgiAgentTab(viewModel: ShivaiViewModel) {
    val context = LocalContext.current
    val agiState by viewModel.agiState.collectAsState()
    val pastGoals by viewModel.agiGoals.collectAsState()

    var goalInput by remember { mutableStateOf("") }

    val presetMissions = listOf(
        "Audit device security, check threats on DuckDuckGo, and create hardening note",
        "Scan system memory, search latest AI security developments, and summarize",
        "Perform deep hardware & network inspection and store security audit"
    )

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AGI Mind Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberCard)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("AUTONOMOUS AGI AGENT CORE", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("ReAct Reasoning & Multi-Step Execution", color = NeonCyan, fontSize = 10.sp)
                        }
                    }

                    if (agiState.isExecuting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NeonCyan, strokeWidth = 2.dp)
                    }
                }

                Text(
                    text = "स्वायत्त एआई एजेंट (Autonomous Agent) आपके जटिल लक्ष्यों को स्वतः छोटे चरणों में तोड़कर, टूल्स चलाकर, परिणाम जांचकर और सीखकर पूरा करता है।",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Mission Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberCard)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Assign Autonomous Mission (लक्ष्य सौंपें):", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    placeholder = { Text("e.g. Audit security, search threats on DuckDuckGo, and save note", color = TextSecondary, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CyberBlack.copy(alpha = 0.5f),
                        unfocusedContainerColor = CyberBlack.copy(alpha = 0.5f)
                    )
                )

                Text("Quick Presets (त्वरित लक्ष्य):", color = TextSecondary, fontSize = 10.sp)
                presetMissions.forEach { preset ->
                    FilterChip(
                        selected = goalInput == preset,
                        onClick = { goalInput = preset },
                        label = { Text(preset.take(38) + "...", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan
                        )
                    )
                }

                Button(
                    onClick = {
                        if (goalInput.isNotBlank()) {
                            viewModel.launchAgiMission(goalInput)
                        } else {
                            Toast.makeText(context, "कृपया लक्ष्य दर्ज करें।", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !agiState.isExecuting && goalInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (agiState.isExecuting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Autonomous Mind Executing...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LAUNCH AGI MISSION (मिशन शुरू करें)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Live AGI Execution HUD
        if (agiState.isExecuting || agiState.steps.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                        Text("ACTIVE THOUGHT & MILESTONES", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Thought Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🧠 Internal Monologue (विचार प्रक्रिया):", color = NeonGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = agiState.currentThought,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Steps Progress Checklist
                    if (agiState.steps.isNotEmpty()) {
                        Text("MISSION MILESTONES:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        agiState.steps.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberCard)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (step.isCompleted) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = if (step.isCompleted) NeonGreen else NeonGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(step.title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    if (step.result.isNotBlank()) {
                                        Text(step.result, color = NeonCyan, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Final output
                    if (agiState.finalOutput.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = agiState.finalOutput,
                                color = Color(0xFF50FA7B),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Past Completed Missions
        if (pastGoals.isNotEmpty()) {
            Text("COMPLETED MISSIONS LOG (${pastGoals.size})", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            pastGoals.take(5).forEach { goal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                            Text(goal.goalTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("Status: ${goal.status} • Completed ${goal.currentStepIndex} Milestones", color = NeonCyan, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
