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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LanguageLexiconTab(viewModel: ShivaiViewModel) {
    val context = LocalContext.current
    val lexiconEntries by viewModel.languageLexicon.collectAsState()

    var languageName by remember { mutableStateOf("") }
    var wordOrPhrase by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var exampleUsage by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, NeonGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
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
                                .background(NeonGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = NeonGold, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("ADAPTIVE LINGUISTIC BRAIN", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("भाषा व शब्दकोश विस्तार (Self-Learning)", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = { isAddingNew = !isAddingNew },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAddingNew) "Close" else "Add New", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Text(
                    text = "💡 शिवाय आपकी किसी भी क्षेत्रीय भाषा (भोजपुरी, मैथिली, मारवाड़ी, संस्कृत, अवधि आदि) या तकनीकी शब्दों को सीख सकता है। जोड़े गए शब्द तुरंत शिवाय की चेतना और वॉयस मॉडल में एकीकृत हो जाते हैं।",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Add New Form
        if (isAddingNew) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("नई भाषा या शब्द जोड़ें:", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = languageName,
                        onValueChange = { languageName = it },
                        label = { Text("Language / Dialect (e.g. Bhojpuri, Maithili)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGold,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = wordOrPhrase,
                        onValueChange = { wordOrPhrase = it },
                        label = { Text("Word / Phrase (शब्द या मुहावरा, e.g. रउवा कइसन बानी)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGold,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = meaning,
                        onValueChange = { meaning = it },
                        label = { Text("Meaning / Translation (अर्थ, e.g. आप कैसे हैं)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGold,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = exampleUsage,
                        onValueChange = { exampleUsage = it },
                        label = { Text("Usage Example (उदाहरण/वाक्य)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGold,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (languageName.isNotBlank() && wordOrPhrase.isNotBlank() && meaning.isNotBlank()) {
                                viewModel.addLanguageLexicon(languageName, wordOrPhrase, meaning, exampleUsage)
                                languageName = ""
                                wordOrPhrase = ""
                                meaning = ""
                                exampleUsage = ""
                                isAddingNew = false
                                Toast.makeText(context, "नया शब्द शिवाय के दिमाग में सेव हो गया!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "कृपया भाषा, शब्द और अर्थ भरें।", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE TO SHIVAI BRAIN (दिमाग में जोड़ें)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of Learned Words
        Text("LEARNED VOCABULARY (${lexiconEntries.size} ITEMS)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        if (lexiconEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberCard)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                    Text("अभी कोई कस्टम शब्द नहीं जोड़ा गया है।", color = TextSecondary, fontSize = 12.sp)
                    Text("'Add New' बटन दबाकर नई भाषा या मुहावरे सिखाएं!", color = NeonGold, fontSize = 11.sp)
                }
            }
        } else {
            lexiconEntries.forEach { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonPurple.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(entry.languageName, color = NeonPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(entry.wordOrPhrase, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text("अर्थ: ${entry.meaning}", color = NeonCyan, fontSize = 11.sp)
                            if (entry.usageExample.isNotBlank()) {
                                Text("प्रयोग: ${entry.usageExample}", color = TextSecondary, fontSize = 10.sp)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteLanguageLexicon(entry.id) }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
