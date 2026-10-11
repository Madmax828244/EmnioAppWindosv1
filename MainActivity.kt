package com.emnio.windowspc

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emnio.windowspc.ui.*
import com.emnio.windowspc.viewmodel.HardwareConfigViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())            // modo imersivo
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { EmnioTheme { AppNav() } }
    }
}

@Composable
fun AppNav(vm: HardwareConfigViewModel = viewModel()) {
    val nav = rememberNavController()
    val activity = LocalContext.current as Activity
    val cfg by vm.config.collectAsState()

    NavHost(nav, startDestination = "config") {
        composable("config") {
            Orient(activity, ActivityInfo.SCREEN_ORIENTATION_USER)
            ConfigScreenUI(vm) { nav.navigate("boot") }
        }
        composable("boot") {
            Orient(activity, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
            BootScreenUI(cfg) { nav.navigate("desktop") { popUpTo("boot") { inclusive = true } } }
        }
        composable("desktop") {
            Orient(activity, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
            WindowsDesktopScreenUI(cfg) { nav.navigate("config") { popUpTo("config") { inclusive = true } } }
        }
    }
}

@Composable
private fun Orient(activity: Activity, orientation: Int) {
    LaunchedEffect(orientation) { activity.requestedOrientation = orientation }
}
