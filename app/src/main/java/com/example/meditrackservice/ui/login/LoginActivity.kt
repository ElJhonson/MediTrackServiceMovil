package com.example.meditrackservice.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.ui.main.MainActivity
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import com.example.meditrackservice.data.model.RefreshRequest
import com.example.meditrackservice.ui.theme.MediTrackServiceTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(application)
    }

    // ui/login/LoginActivity.kt
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val token = TokenDataStoreProvider.getInstance(this@LoginActivity)
                .accessToken.first()
            val refreshToken = TokenDataStoreProvider.getInstance(this@LoginActivity)
                .refreshToken.first()

            when {
                // Sin token → mostrar login
                token.isNullOrBlank() -> mostrarLogin()

                // Hay refresh token → intentar renovar antes de ir a Main
                !refreshToken.isNullOrBlank() -> {
                    val tokenValido = verificarORefrescarToken(token, refreshToken)
                    if (tokenValido) irAMain()
                    else mostrarLogin()
                }

                else -> mostrarLogin()
            }
        }
    }

    private suspend fun verificarORefrescarToken(
        token: String,
        refreshToken: String
    ): Boolean {
        return try {
            val tempApi = RetrofitClient.create(
                tokenProvider = { null },
                refreshTokenProvider = { null },
                onTokenRefreshed = {},
                onSessionExpired = {}
            )
            val response = tempApi.refresh(RefreshRequest(refreshToken))
            // Guardar nuevo token
            TokenDataStoreProvider.getInstance(this@LoginActivity)
                .guardarTokens(response.accessToken, refreshToken)
            true
        } catch (e: Exception) {
            Log.e("LoginActivity", "No se pudo refrescar: ${e.message}")
            false
        }
    }

    private fun mostrarLogin() {
        setContent {
            MediTrackServiceTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { irAMain() }
                )
            }
        }
    }


    private fun irAMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}