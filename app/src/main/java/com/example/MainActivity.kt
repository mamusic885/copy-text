package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainScreen
import com.example.ui.theme.CopyTextTheme
import com.example.viewmodel.OcrViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CopyTextTheme {
        val ocrViewModel: OcrViewModel = viewModel()
        MainScreen(ocrViewModel = ocrViewModel)
      }
    }
  }
}

