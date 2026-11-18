package com.example

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.canvas.CircuitWorkspaceScreen
import com.example.ui.export.ProjectExportSheet
import com.example.ui.instruments.VirtualInstrumentsSheet
import com.example.ui.modules.BreadboardScreen
import com.example.ui.modules.PcbDesignerScreen
import com.example.ui.screens.AiAssistantSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LabChallengesScreen
import com.example.ui.screens.LearningCenterScreen
import com.example.ui.screens.LibraryAndToolsScreen
import com.example.ui.screens.PreOpeningSplashScreen
import com.example.ui.screens.ProjectsAndModulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.SalmanDigitalTheme
import com.example.ui.theme.ThemePickerSheet
import com.example.viewmodel.AppTab
import com.example.viewmodel.WorkbenchViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            SalmanDigitalApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalmanDigitalApp(vm: WorkbenchViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val currentTab by vm.currentTab.collectAsStateWithLifecycle()
    val subScreen by vm.subScreen.collectAsStateWithLifecycle()
    val statusBanner by vm.statusBanner.collectAsStateWithLifecycle()

    var showInstruments by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var isWorkspaceFullScreen by remember { mutableStateOf(false) }
    var showPreOpeningSplash by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentTab, subScreen) {
        if (currentTab != AppTab.DESIGN || subScreen != null) {
            isWorkspaceFullScreen = false
        }
    }
    val fullScreenActive = isWorkspaceFullScreen && currentTab == AppTab.DESIGN && subScreen == null

    val activeTheme = remember(settings.themeId, settings.isDarkTheme) {
        AppThemePreset.fromId(settings.themeId, fallbackDark = settings.isDarkTheme)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.importProjectUri(uri)
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.clearStatusBanner()
        }
    }

    val context = LocalContext.current
    val view = LocalView.current
    SideEffect {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !activeTheme.isDark
            controller.isAppearanceLightNavigationBars = !activeTheme.isDark
        }
    }

    LaunchedEffect(settings.screenOrientation) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val target = when (settings.screenOrientation) {
            "AUTO" -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (activity.requestedOrientation != target) {
            activity.requestedOrientation = target
        }
    }

    if (subScreen != null) {
        BackHandler { vm.openSubScreen(null) }
    } else if (fullScreenActive) {
        BackHandler { isWorkspaceFullScreen = false }
    }

    val appStrings = remember(settings.language) { AppStrings.forLanguage(settings.language) }

    CompositionLocalProvider(LocalAppStrings provides appStrings) {
        SalmanDigitalTheme(themePreset = activeTheme) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpanded = maxWidth >= 700.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!isExpanded && subScreen == null && !fullScreenActive) {
                            NavigationBar {
                                AppTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    val (filledIcon, outlinedIcon) = tabIcons(tab)
                                    val localizedLabel = tabLabel(tab, appStrings)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { vm.navigateTab(tab) },
                                        icon = {
                                            Icon(
                                                if (selected) filledIcon else outlinedIcon,
                                                contentDescription = localizedLabel
                                            )
                                        },
                                        label = { Text(localizedLabel) },
                                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isExpanded && subScreen == null && !fullScreenActive) {
                            NavigationRail(modifier = Modifier.fillMaxHeight()) {
                                AppTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    val (filledIcon, outlinedIcon) = tabIcons(tab)
                                    val localizedLabel = tabLabel(tab, appStrings)
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { vm.navigateTab(tab) },
                                        icon = {
                                            Icon(
                                                if (selected) filledIcon else outlinedIcon,
                                                contentDescription = localizedLabel
                                            )
                                        },
                                        label = { Text(localizedLabel) },
                                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        when (subScreen) {
                            "breadboard" -> BreadboardScreen(vm = vm, onBack = { vm.openSubScreen(null) })
                            "pcb" -> PcbDesignerScreen(vm = vm, onBack = { vm.openSubScreen(null) })
                            "learning_center" -> LearningCenterScreen(vm = vm, onBack = { vm.openSubScreen(null) })
                            "challenges" -> LabChallengesScreen(vm = vm, onBack = { vm.openSubScreen(null) })
                            else -> when (currentTab) {
                                AppTab.HOME -> DashboardScreen(
                                    vm = vm,
                                    onRequestImportFile = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                                    onOpenThemePicker = { showThemeSheet = true }
                                )
                                AppTab.DESIGN -> CircuitWorkspaceScreen(
                                    vm = vm,
                                    onOpenInstruments = { showInstruments = true },
                                    onOpenAiAssistant = { showAiAssistant = true },
                                    onOpenExport = { showExportSheet = true },
                                    onOpenThemePicker = { showThemeSheet = true },
                                    isFullScreen = fullScreenActive,
                                    onToggleFullScreen = { isWorkspaceFullScreen = !isWorkspaceFullScreen }
                                )
                                AppTab.LIBRARY -> LibraryAndToolsScreen(vm = vm)
                                AppTab.PROJECTS -> ProjectsAndModulesScreen(
                                    vm = vm,
                                    onRequestImportFile = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                                )
                                AppTab.SETTINGS -> SettingsScreen(
                                    vm = vm,
                                    onOpenThemePicker = { showThemeSheet = true }
                                )
                            }
                        }
                    }
                }
            }

            if (showInstruments) {
                ModalBottomSheet(onDismissRequest = { showInstruments = false }) {
                    VirtualInstrumentsSheet(vm = vm, onClose = { showInstruments = false })
                }
            }

            if (showAiAssistant) {
                ModalBottomSheet(onDismissRequest = { showAiAssistant = false }) {
                    AiAssistantSheet(vm = vm, onClose = { showAiAssistant = false })
                }
            }

            if (showExportSheet) {
                ModalBottomSheet(onDismissRequest = { showExportSheet = false }) {
                    ProjectExportSheet(vm = vm, onClose = { showExportSheet = false })
                }
            }

            if (showThemeSheet) {
                ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
                    ThemePickerSheet(
                        selectedThemeId = activeTheme.id,
                        onSelectTheme = { preset -> vm.selectTheme(preset) },
                        onClose = { showThemeSheet = false }
                    )
                }
            }

            AnimatedVisibility(
                visible = showPreOpeningSplash,
                exit = fadeOut(animationSpec = tween(durationMillis = 420))
            ) {
                PreOpeningSplashScreen(
                    onFinished = { showPreOpeningSplash = false }
                )
            }
        }
    }
    }
}

private fun tabIcons(tab: AppTab) = when (tab) {
    AppTab.HOME -> Icons.Filled.Home to Icons.Outlined.Home
    AppTab.DESIGN -> Icons.Filled.DeveloperBoard to Icons.Outlined.DeveloperBoard
    AppTab.LIBRARY -> Icons.Filled.Memory to Icons.Outlined.Memory
    AppTab.PROJECTS -> Icons.Filled.Folder to Icons.Outlined.Folder
    AppTab.SETTINGS -> Icons.Filled.Settings to Icons.Outlined.Settings
}

private fun tabLabel(tab: AppTab, strings: AppStrings): String = when (tab) {
    AppTab.HOME -> strings.navHome
    AppTab.DESIGN -> strings.navDesign
    AppTab.LIBRARY -> strings.navLibrary
    AppTab.PROJECTS -> strings.navProjects
    AppTab.SETTINGS -> strings.navSettings
}
