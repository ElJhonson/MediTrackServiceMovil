package com.example.meditrackservice.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.example.meditrackservice.ui.main.MainActivity
import com.example.meditrackservice.data.local.TokenDataStore
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya hay sesión activa
        lifecycleScope.launch {
            val token = TokenDataStoreProvider.getInstance(this@LoginActivity)
                .accessToken.first()

            if (!token.isNullOrBlank()) {
                irAMain()
                return@launch
            }

            // Montar la UI con Compose
            setContent {
                MaterialTheme {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { irAMain() }
                    )
                }
            }
        }
    }

    private fun irAMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}