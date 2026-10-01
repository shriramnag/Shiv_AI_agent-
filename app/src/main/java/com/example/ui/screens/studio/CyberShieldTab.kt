package com.example.ui.screens.studio

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.ThreatLevel
import com.example.ui.ShivaiViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CyberShieldTab(viewModel: ShivaiViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val securityReport by viewModel.deviceSecurityReport.collectAsState()
    val lastUrlScan by viewModel.lastUrlScan.collectAsState()
    val lastFraudScan by viewModel.lastFraudScan.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (securityReport == null) {
            viewModel.runDeviceSecurityAudit()
        }
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Device Security Posture Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberCard)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            Icon(Icons.Default.Shield, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("CYBER SHIELD POSTURE", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Real-Time Device Integrity", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    securityReport?.let { report ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (report.overallSecurityScore >= 80) NeonGreen.copy(alpha = 0.2f) else NeonGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${report.overallSecurityScore}% SHIELD",
                                color = if (report.overallSecurityScore >= 80) NeonGreen else NeonGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                securityReport?.let { report ->
                    Text(text = report.securityStatusLabel, color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Root Superuser:", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = if (report.isRooted) "DANGER (DETECTED)" else "SAFE (UNROOTED)",
                            color = if (report.isRooted) NeonRed else NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("USB Debugging (ADB):", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = if (report.isUsbDebuggingActive) "ACTIVE (DEV MODE)" else "SECURE (OFF)",
                            color = if (report.isUsbDebuggingActive) NeonGold else NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("VPN / Network Protection:", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = if (report.isVpnActive) "VPN ACTIVE" else "STANDARD NETWORK",
                            color = if (report.isVpnActive) NeonCyan else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.runDeviceSecurityAudit() },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RE-AUDIT SECURITY SHIELD (सुरक्षा जांचें)", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Malicious Link & Phishing Scanner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberCard)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = NeonGold, modifier = Modifier.size(20.dp))
                    Text("PHISHING LINK SCANNER (लिंक स्कैनर)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("Paste suspicious link (संदेहास्पद लिंक पेस्ट करें)", color = TextSecondary, fontSize = 12.sp) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text?.trim()
                                if (!clip.isNullOrBlank()) {
                                    urlInput = clip
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = NeonCyan)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
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

                Button(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            viewModel.scanUrl(urlInput)
                        } else {
                            Toast.makeText(context, "कृपया पहले लिंक दर्ज करें।", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SCAN LINK FOR THREATS (जांचें)", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                lastUrlScan?.let { result ->
                    val badgeColor = when (result.threatLevel) {
                        ThreatLevel.MALWARE_THREAT -> NeonRed
                        ThreatLevel.HIGH_RISK_FRAUD -> NeonRed
                        ThreatLevel.CAUTION -> NeonGold
                        ThreatLevel.SAFE -> NeonGreen
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (result.isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "THREAT LEVEL: ${result.threatLevel.name} (SCORE: ${result.threatScore}%)",
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(text = result.recommendation, color = TextPrimary, fontSize = 12.sp)
                            if (result.detectedThreats.isNotEmpty()) {
                                Text("पहचाने गए खतरे:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                result.detectedThreats.forEach { threat ->
                                    Text("• $threat", color = NeonRed, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Banking & SMS Fraud Message Scanner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberCard)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = NeonRed, modifier = Modifier.size(20.dp))
                    Text("BANKING & FRAUD SMS ANALYZER", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text("Paste suspicious SMS / WhatsApp text (संदेश पेस्ट करें)", color = TextSecondary, fontSize = 12.sp) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text?.trim()
                                if (!clip.isNullOrBlank()) {
                                    messageInput = clip
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = NeonCyan)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CyberBlack.copy(alpha = 0.5f),
                        unfocusedContainerColor = CyberBlack.copy(alpha = 0.5f)
                    ),
                    maxLines = 4
                )

                Button(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            viewModel.scanMessageForFraud(messageInput)
                        } else {
                            Toast.makeText(context, "कृपया पहले संदेश दर्ज करें।", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DETECT FINANCIAL SCAMS (फ़्रॉड की जांच करें)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                lastFraudScan?.let { result ->
                    val cardColor = if (result.isFraud) NeonRed else NeonGreen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardColor.copy(alpha = 0.15f))
                            .border(1.dp, cardColor, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = result.warningMessage, color = cardColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = "प्रकार: ${result.scamType}", color = TextSecondary, fontSize = 11.sp)
                            Text(text = result.defensiveAction, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 4. Emergency Cyber Helpline (1930) Direct Connect
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, NeonRed.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("राष्ट्रीय साइबर हेल्पलाइन (National Cyber Crime)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("वित्तीय धोखाधड़ी की तुरंत रिपोर्ट करने के लिए 1930 पर कॉल करें।", color = TextSecondary, fontSize = 10.sp)
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("1930", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
