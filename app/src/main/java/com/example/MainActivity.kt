package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.ShivaiViewModel
import com.example.ui.components.SensitiveActionDialog
import com.example.ui.screens.CallingModeScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioHubScreen
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class ShivaiTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CHAT("Chat", Icons.Default.Chat),
    STUDIO("Studio", Icons.Default.AutoAwesome),
    NOTES("Notes", Icons.Default.NoteAlt),
    MEMORY("Memory", Icons.Default.Psychology),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ShivaiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: ShivaiViewModel) {
    var currentTab by remember { mutableStateOf(ShivaiTab.HOME) }
    val isCallingModeActive by viewModel.isCallingModeActive.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val pendingConfirmation by ShivaiApplication.instance.pendingConfirmation.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launcher
    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    // Handle back navigation
    BackHandler(enabled = isCallingModeActive || currentTab != ShivaiTab.HOME) {
        if (isCallingModeActive) {
            viewModel.stopCallingMode()
        } else if (currentTab != ShivaiTab.HOME) {
            currentTab = ShivaiTab.HOME
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isCallingModeActive) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder),
                        containerColor = CyberSurface
                    ) {
                        ShivaiTab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = CyberBorder
                                ),
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    ShivaiTab.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToCallingMode = { viewModel.startCallingMode() },
                        onNavigateToChat = { currentTab = ShivaiTab.CHAT },
                        onNavigateToStudio = { currentTab = ShivaiTab.STUDIO },
                        onNavigateToNotes = { currentTab = ShivaiTab.NOTES },
                        onNavigateToMemory = { currentTab = ShivaiTab.MEMORY },
                        onNavigateToSettings = { currentTab = ShivaiTab.SETTINGS }
                    )
                    ShivaiTab.CHAT -> ChatScreen(viewModel = viewModel)
                    ShivaiTab.STUDIO -> StudioHubScreen(viewModel = viewModel)
                    ShivaiTab.NOTES -> NotesScreen(viewModel = viewModel)
                    ShivaiTab.MEMORY -> MemoryScreen(viewModel = viewModel)
                    ShivaiTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentTab = ShivaiTab.HOME }
                    )
                }
            }
        }

        // Fullscreen Calling Mode overlay
        if (isCallingModeActive) {
            CallingModeScreen(
                viewModel = viewModel,
                onEndCall = { viewModel.stopCallingMode() }
            )
        }

        // Sensitive Action Confirmation Dialog
        pendingConfirmation?.let { req ->
            SensitiveActionDialog(
                request = req,
                onDismiss = { ShivaiApplication.instance.clearConfirmation() }
            )
        }
    }
}
