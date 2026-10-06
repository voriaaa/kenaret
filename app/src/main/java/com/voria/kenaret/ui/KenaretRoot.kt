package com.voria.kenaret.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.voria.kenaret.settings.LocaleManager
import com.voria.kenaret.ui.main.MainScreen
import com.voria.kenaret.ui.onboarding.HerSetupScreen
import com.voria.kenaret.ui.onboarding.PartnerSetupScreen
import com.voria.kenaret.ui.onboarding.RoleScreen
import com.voria.kenaret.ui.onboarding.WelcomeScreen
import com.voria.kenaret.ui.settings.AboutScreen
import com.voria.kenaret.ui.settings.CycleSettingsScreen
import com.voria.kenaret.ui.settings.DisclaimerScreen
import com.voria.kenaret.ui.settings.NotificationSettingsScreen
import com.voria.kenaret.ui.settings.PartnerSettingsScreen
import com.voria.kenaret.ui.settings.PrivacyScreen
import com.voria.kenaret.ui.theme.KenaretTheme

object Routes {
    const val WELCOME = "welcome"
    const val ROLE = "role"
    const val SETUP_HER = "setup_her"
    const val SETUP_PARTNER = "setup_partner"
    const val MAIN = "main"
    const val SETTINGS_CYCLE = "settings_cycle"
    const val SETTINGS_NOTIFICATIONS = "settings_notifications"
    const val SETTINGS_PARTNER = "settings_partner"
    const val SETTINGS_PRIVACY = "settings_privacy"
    const val ABOUT = "about"
    const val DISCLAIMER = "disclaimer"
}

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** Switches the in-app language and recreates the activity so RTL / LTR and strings update. */
fun changeLanguage(context: Context, language: String) {
    LocaleManager.setLanguage(context, language)
    context.findActivity()?.recreate()
}

fun NavHostController.navigateClearing(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun KenaretRoot() {
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    KenaretTheme(themeMode = state.themeMode) {
        SystemBarsAppearance()
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (state.loaded) {
                val navController = rememberNavController()
                val start = remember { if (state.onboarded) Routes.MAIN else Routes.WELCOME }
                val context = LocalContext.current
                val back: () -> Unit = { navController.popBackStack() }
                val onDataDeleted: () -> Unit = {
                    navController.navigateClearing(Routes.WELCOME)
                    // Language preference is reset as well; recreate so the UI follows it.
                    context.findActivity()?.recreate()
                }

                NavHost(navController = navController, startDestination = start) {
                    composable(Routes.WELCOME) {
                        WelcomeScreen(onStart = { navController.navigate(Routes.ROLE) })
                    }
                    composable(Routes.ROLE) {
                        RoleScreen(
                            onBack = back,
                            onHer = { navController.navigate(Routes.SETUP_HER) },
                            onPartner = { navController.navigate(Routes.SETUP_PARTNER) },
                        )
                    }
                    composable(Routes.SETUP_HER) {
                        HerSetupScreen(
                            today = state.today,
                            onBack = back,
                            onFinish = { start, cycle, period, unknown ->
                                viewModel.completeHerOnboarding(start, cycle, period, unknown) {
                                    navController.navigateClearing(Routes.MAIN)
                                }
                            },
                        )
                    }
                    composable(Routes.SETUP_PARTNER) {
                        PartnerSetupScreen(
                            onBack = back,
                            onFinish = { data ->
                                viewModel.completePartnerOnboarding(data) {
                                    navController.navigateClearing(Routes.MAIN)
                                }
                            },
                        )
                    }
                    composable(Routes.MAIN) {
                        MainScreen(
                            state = state,
                            viewModel = viewModel,
                            onNavigate = { route -> navController.navigate(route) },
                            onDataDeleted = onDataDeleted,
                        )
                    }
                    composable(Routes.SETTINGS_CYCLE) {
                        CycleSettingsScreen(state = state, onBack = back, onSave = { c, p, u ->
                            viewModel.updateCycleSettings(c, p, u)
                            back()
                        })
                    }
                    composable(Routes.SETTINGS_NOTIFICATIONS) {
                        NotificationSettingsScreen(state = state, onBack = back, onChange = viewModel::updateNotifications)
                    }
                    composable(Routes.SETTINGS_PARTNER) {
                        PartnerSettingsScreen(state = state, viewModel = viewModel, onBack = back)
                    }
                    composable(Routes.SETTINGS_PRIVACY) {
                        PrivacyScreen(
                            state = state,
                            viewModel = viewModel,
                            onBack = back,
                            onDataDeleted = onDataDeleted,
                        )
                    }
                    composable(Routes.ABOUT) { AboutScreen(onBack = back) }
                    composable(Routes.DISCLAIMER) { DisclaimerScreen(onBack = back) }
                }
            }
        }
    }
}

@Composable
private fun SystemBarsAppearance() {
    val view = LocalView.current
    val context = LocalContext.current
    val lightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
    if (!view.isInEditMode) {
        SideEffect {
            val window = context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightBackground
                isAppearanceLightNavigationBars = lightBackground
            }
        }
    }
}
