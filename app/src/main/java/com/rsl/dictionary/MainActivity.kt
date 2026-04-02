package com.rsl.dictionary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rsl.dictionary.ui.screens.MainScreen
import com.rsl.dictionary.ui.theme.RussianSignLanguageDictionaryAndroidTheme as RslDictionaryTheme
import com.rsl.dictionary.viewmodels.StartupViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val startupViewModel: StartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            RslDictionaryTheme {
                MainScreen(startupViewModel = startupViewModel)
            }
        }
    }
}
