package com.zak.pressmark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.core.ui.theme.PressmarkTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PressmarkTheme(dynamicColor = false) {
                val graph = remember { AppGraph(applicationContext) }
                PressmarkApp(graph = graph)
            }
        }
    }
}
