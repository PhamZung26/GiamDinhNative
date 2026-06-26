package com.tc128.giamdinhnative

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tc128.giamdinhnative.ui.navigation.AppNavGraph
import com.tc128.giamdinhnative.ui.theme.GiamDinhNativeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GiamDinhNativeTheme {
                AppNavGraph()
            }
        }
    }
}
