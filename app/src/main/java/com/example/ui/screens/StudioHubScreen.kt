package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import java.io.InputStream

@Composable
fun StudioHubScreen(
    viewModel: ShivaiViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val isGenerating by viewModel.isGeneratingAsset.collectAsState()

    val tabs = listOf(
        "AGI Agent",
        "Cyber Shield",
        "Code Studio",
        "Language Brain",
        "Vision AI",
        "Image Studio",
        "Veo Video",
        "Lyria Music",
        "Grounding",
        "Smart Home",
        "Knowledge Vault",
        "Meeting Scribe"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Futuristic Studio Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberSurface)
                .border(1.dp, CyberBorder)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Shivai Generative Studio",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = NeonCyan,
                    strokeWidth = 2.dp
                )
            }
        }

        // Horizontal Category Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = CyberSurface,
            contentColor = NeonCyan,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonCyan
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) NeonCyan else TextSecondary
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            when (selectedTab) {
                0 -> com.example.ui.screens.studio.AgiAgentTab(viewModel)
                1 -> com.example.ui.screens.studio.CyberShieldTab(viewModel)
                2 -> com.example.ui.screens.studio.CodingStudioTab(viewModel)
                3 -> com.example.ui.screens.studio.LanguageLexiconTab(viewModel)
                4 -> VisionAiTab(viewModel)
                5 -> ImageStudioTab(viewModel)
                6 -> VeoVideoTab(viewModel)
                7 -> LyriaMusicTab(viewModel)
                8 -> GroundingTab(viewModel)
                9 -> SmartHomeTab(viewModel)
                10 -> KnowledgeVaultTab(viewModel)
                11 -> MeetingScribeTab(viewModel)
            }
        }
    }
}

// --- 1. VISION AI TAB ---
@Composable
private fun VisionAiTab(viewModel: ShivaiViewModel) {
    val context = LocalContext.current
    val isGenerating by viewModel.isGeneratingAsset.collectAsState()
    val visionResult by viewModel.visionAnalysisResult.collectAsState()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var visionPrompt by remember { mutableStateOf("Describe what you see and highlight key details.") }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            viewModel.analyzeCameraVision(bitmap, visionPrompt)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    viewModel.analyzeCameraVision(bitmap, visionPrompt)
                }
            } catch (e: Exception) {
                // Handle read error
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Live Multimodal Camera Vision",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Capture a photo or upload an image. Shivai will visually inspect and explain it using Gemini 3.5 Flash.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        OutlinedTextField(
            value = visionPrompt,
            onValueChange = { visionPrompt = it },
            label = { Text("What should Shivai look for?") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { takePictureLauncher.launch(null) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take Photo", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { pickImageLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick Image", color = NeonCyan)
            }
        }

        capturedBitmap?.let { bmp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Analyzed Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }

        if (isGenerating) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                Text("Shivai is analyzing the image...", color = NeonCyan, fontSize = 13.sp)
            }
        }

        visionResult?.let { resultText ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SHIVAI VISUAL ANALYSIS", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(resultText, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

// --- 2. IMAGE STUDIO TAB ---
@Composable
private fun ImageStudioTab(viewModel: ShivaiViewModel) {
    val isGenerating by viewModel.isGeneratingAsset.collectAsState()
    val imageB64 by viewModel.generatedImageBase64.collectAsState()

    var prompt by remember { mutableStateOf("A futuristic holographic neon cyber city with flying vehicles") }
    var selectedRatio by remember { mutableStateOf("1:1") }

    val ratios = listOf("1:1", "16:9", "9:16", "4:3", "3:4")

    val decodedBitmap = remember(imageB64) {
        if (imageB64 != null) {
            try {
                val bytes = Base64.decode(imageB64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) { null }
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("AI Image Studio", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Powered by gemini-3.1-flash-image-preview", color = NeonPurple, fontSize = 12.sp)

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Image Description Prompt") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonPurple,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Text("Aspect Ratio", color = TextSecondary, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ratios.forEach { ratio ->
                FilterChip(
                    selected = selectedRatio == ratio,
                    onClick = { selectedRatio = ratio },
                    label = { Text(ratio) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Button(
            onClick = { viewModel.generateImage(prompt, null, selectedRatio) },
            enabled = !isGenerating && prompt.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Creating Image...", color = Color.White)
            } else {
                Text("Generate 1K Image", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        decodedBitmap?.let { bmp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Generated Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Generated with gemini-3.1-flash-image-preview", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// --- 3. VEO VIDEO TAB ---
@Composable
private fun VeoVideoTab(viewModel: ShivaiViewModel) {
    val isGenerating by viewModel.isGeneratingAsset.collectAsState()
    val videoInfo by viewModel.generatedVideoInfo.collectAsState()

    var prompt by remember { mutableStateOf("Cinematic slow-motion shot of a glowing AI cyber crystal rotating in space") }
    var selectedRatio by remember { mutableStateOf("16:9") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Veo 3 Video Generator", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Model: veo-3.1-fast-generate-preview (16:9 landscape / 9:16 portrait)", color = NeonGold, fontSize = 12.sp)

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Video Prompt") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonGold,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("16:9", "9:16").forEach { ratio ->
                FilterChip(
                    selected = selectedRatio == ratio,
                    onClick = { selectedRatio = ratio },
                    label = { Text(if (ratio == "16:9") "Landscape (16:9)" else "Portrait (9:16)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGold,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Button(
            onClick = { viewModel.generateVideo(prompt, null, selectedRatio) },
            enabled = !isGenerating && prompt.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submitting to Veo...", color = Color.Black)
            } else {
                Text("Generate Veo 3 Video", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        videoInfo?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("VEO 3 GENERATION STATUS", color = NeonGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(info, color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

// --- 4. LYRIA MUSIC TAB ---
@Composable
private fun LyriaMusicTab(viewModel: ShivaiViewModel) {
    val isGenerating by viewModel.isGeneratingAsset.collectAsState()
    val audioB64 by viewModel.generatedAudioBase64.collectAsState()

    var prompt by remember { mutableStateOf("Upbeat futuristic electro synthwave with deep bassline and cyberpunk chords") }
    var isShortClip by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Lyria Music Studio", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Lyria 3 Preview music synthesis for clips & full tracks", color = NeonGreen, fontSize = 12.sp)

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Music Mood & Style Prompt") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = isShortClip,
                onClick = { isShortClip = true },
                label = { Text("Short Clip (lyria-3-clip-preview)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonGreen,
                    selectedLabelColor = Color.Black
                )
            )
            FilterChip(
                selected = !isShortClip,
                onClick = { isShortClip = false },
                label = { Text("Pro (lyria-3-pro-preview)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonGreen,
                    selectedLabelColor = Color.Black
                )
            )
        }

        Button(
            onClick = { viewModel.generateMusic(prompt, isShortClip) },
            enabled = !isGenerating && prompt.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synthesizing Music...", color = Color.Black)
            } else {
                Text("Synthesize Music Track", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        audioB64?.let { b64 ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lyria Audio Generated", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Tap to play synthesized audio stream", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = { viewModel.playAudioTrack(b64) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                    }
                }
            }
        }
    }
}

// --- 5. GROUNDING TAB ---
@Composable
private fun GroundingTab(viewModel: ShivaiViewModel) {
    val isGenerating by viewModel.isGeneratingAsset.collectAsState()
    val groundingResult by viewModel.groundingResult.collectAsState()

    var query by remember { mutableStateOf("Latest news on space exploration and AI breakthroughs") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Google Grounding Intelligence", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Real-time web search and location data via gemini-3.5-flash", color = NeonCyan, fontSize = 12.sp)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Query or Location") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.querySearchGrounding(query) },
                enabled = !isGenerating && query.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search Grounding", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.queryMapsGrounding(query) },
                enabled = !isGenerating && query.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Maps Grounding", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        groundingResult?.let { (text, sources) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GROUNDED RESPONSE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)

                    if (sources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("WEB SOURCES & CITATIONS:", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        sources.forEach { source ->
                            Text("• $source", color = NeonCyan, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// --- 6. SMART HOME IOT TAB ---
@Composable
private fun SmartHomeTab(viewModel: ShivaiViewModel) {
    val devices by viewModel.smartDevices.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Connected IoT Devices", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${devices.size} devices online • Voice controllable via Shivai", color = NeonCyan, fontSize = 12.sp)
            }
        }

        // Quick Scene Triggers
        Text("Smart Scenes", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = false,
                onClick = { viewModel.activateSmartScene("bedtime") },
                label = { Text("Bedtime Mode (Lights Off, AC 24°C)") },
                colors = FilterChipDefaults.filterChipColors(labelColor = NeonPurple)
            )
            FilterChip(
                selected = false,
                onClick = { viewModel.activateSmartScene("movie night") },
                label = { Text("Movie Night (Dim 20%, TV On)") },
                colors = FilterChipDefaults.filterChipColors(labelColor = NeonGold)
            )
        }

        // Devices List
        devices.forEach { dev ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, if (dev.isPoweredOn) NeonCyan.copy(alpha = 0.5f) else CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (dev.isPoweredOn) NeonCyan.copy(alpha = 0.15f) else CyberSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (dev.type) {
                                    "LIGHT" -> Icons.Default.Lightbulb
                                    "AC" -> Icons.Default.AcUnit
                                    "TV" -> Icons.Default.Tv
                                    else -> Icons.Default.Power
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = dev.name,
                                    tint = if (dev.isPoweredOn) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(dev.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${dev.room} • ${if (dev.isPoweredOn) "ON" else "OFF"}", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = dev.isPoweredOn,
                            onCheckedChange = { viewModel.toggleSmartDevice(dev.id, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }

                    if (dev.isPoweredOn && (dev.type == "LIGHT" || dev.type == "AC")) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (dev.type == "LIGHT") "Brightness: ${dev.brightnessOrValue}%" else "Temp: ${dev.brightnessOrValue}°C",
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = dev.brightnessOrValue.toFloat(),
                            onValueChange = { viewModel.setSmartDeviceValue(dev.id, it.toInt()) },
                            valueRange = if (dev.type == "LIGHT") 10f..100f else 16f..30f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = CyberBorder
                            )
                        )
                    }
                }
            }
        }
    }
}

// --- 7. KNOWLEDGE VAULT (RAG) TAB ---
@Composable
private fun KnowledgeVaultTab(viewModel: ShivaiViewModel) {
    val documents by viewModel.documents.collectAsState()
    var docTitle by remember { mutableStateOf("") }
    var docContent by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Private Knowledge Vault (Local RAG)", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Store your private documents, college notes, or policy details. Shivai will read and recall them whenever you ask.", color = NeonGold, fontSize = 12.sp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberCard)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add New Knowledge Document", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { docTitle = it },
                    label = { Text("Document Title (e.g. Health Insurance Policy)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGold,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = docContent,
                    onValueChange = { docContent = it },
                    label = { Text("Document Content / Text Extract") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGold,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 5
                )

                Button(
                    onClick = {
                        if (docTitle.isNotBlank() && docContent.isNotBlank()) {
                            viewModel.addKnowledgeDocument(docTitle.trim(), docContent.trim())
                            docTitle = ""
                            docContent = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Index into Knowledge Vault", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Indexed Documents (${documents.size})", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        documents.forEach { doc ->
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(doc.content.take(120) + "...", color = TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = { viewModel.deleteKnowledgeDocument(doc.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = NeonRed)
                    }
                }
            }
        }
    }
}

// --- 8. MEETING SCRIBE TAB ---
@Composable
private fun MeetingScribeTab(viewModel: ShivaiViewModel) {
    val meetings by viewModel.meetingSummaries.collectAsState()
    val isGenerating by viewModel.isGeneratingAsset.collectAsState()

    var meetingTitle by remember { mutableStateOf("Design & Strategy Sync") }
    var meetingTranscript by remember {
        mutableStateOf("Rahul presented the quarterly metrics. Growth is up 35%. Priyansh will finalize the app architecture by Friday. We agreed to launch the beta test next Monday with 500 users.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AI Meeting & Lecture Scribe", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Generates executive summaries, key bullet points, and action items with Gemini 3.5 Flash.", color = NeonGreen, fontSize = 12.sp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberCard)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = meetingTitle,
                    onValueChange = { meetingTitle = it },
                    label = { Text("Meeting / Lecture Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = meetingTranscript,
                    onValueChange = { meetingTranscript = it },
                    label = { Text("Spoken Transcript / Discussion Notes") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines = 6
                )

                Button(
                    onClick = { viewModel.createMeetingSummary(meetingTitle, meetingTranscript) },
                    enabled = !isGenerating && meetingTranscript.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Drafting Minutes...", color = Color.Black)
                    } else {
                        Text("Generate Structured Minutes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text("Saved Meeting Minutes (${meetings.size})", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        meetings.forEach { meeting ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(meeting.title, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        IconButton(onClick = { viewModel.deleteMeetingSummary(meeting.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = NeonRed)
                        }
                    }

                    if (meeting.executiveSummary.isNotBlank()) {
                        Text("EXECUTIVE SUMMARY", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(meeting.executiveSummary, color = TextPrimary, fontSize = 13.sp)
                    }

                    if (meeting.actionItems.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ACTION ITEMS", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(meeting.actionItems, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

