package com.example.meditrackservice.ui.historial


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.meditrackservice.ui.theme.MediTrackServiceTheme

class HistorialActivity : ComponentActivity() {

    private val viewModel: HistorialViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HistorialViewModel(application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediTrackServiceTheme {
                HistorialScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}