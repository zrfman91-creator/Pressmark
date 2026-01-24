// FILE: app/src/main/java/com/zak/pressmark/app/MainActivity.kt
package com.zak.pressmark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import com.zak.pressmark.core.ui.theme.PressmarkTheme
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PressmarkTheme(dynamicColor = false) {
                PressmarkApp()
            }
        }
    }
}
