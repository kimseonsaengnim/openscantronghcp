package com.omrgrader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import com.omrgrader.presentation.OpenScantronApp
import com.omrgrader.presentation.theme.OpenScantronTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenScantronTheme {
                OpenScantronApp()
            }
        }
    }
}
