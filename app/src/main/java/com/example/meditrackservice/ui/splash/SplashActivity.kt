// ui/splash/SplashActivity.kt
package com.example.meditrackservice.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.meditrackservice.R
import com.example.meditrackservice.alarm.AlarmForegroundService
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import com.example.meditrackservice.ui.login.LoginActivity
import com.example.meditrackservice.ui.main.MainActivity
import com.example.meditrackservice.ui.theme.MediTrackServiceTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent(this, AlarmForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        setContent {
            MediTrackServiceTheme {
                SplashScreen()
            }
        }

        lifecycleScope.launch {
            delay(2500) // ← mostrar splash 2.5 segundos
            verificarSesion()
        }
    }

    private suspend fun verificarSesion() {
        val tokenDataStore = TokenDataStoreProvider.getInstance(this)
        val token = tokenDataStore.accessToken.first()
        val refreshToken = tokenDataStore.refreshToken.first()

        when {
            token.isNullOrBlank() -> irA(LoginActivity::class.java)

            !refreshToken.isNullOrBlank() -> {
                val tokenValido = intentarRefresh(refreshToken)
                if (tokenValido) irA(MainActivity::class.java)
                else irA(LoginActivity::class.java)
            }

            else -> irA(LoginActivity::class.java)
        }
    }

    private suspend fun intentarRefresh(refreshToken: String): Boolean {
        return try {
            val tempApi = com.example.meditrackservice.data.api.RetrofitClient.create(
                tokenProvider = { null },
                refreshTokenProvider = { null },
                onTokenRefreshed = {},
                onSessionExpired = {}
            )
            val response = tempApi.refresh(
                com.example.meditrackservice.data.model.RefreshRequest(refreshToken)
            )
            TokenDataStoreProvider.getInstance(this)
                .guardarTokens(response.accessToken, refreshToken)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun irA(destino: Class<*>) {
        startActivity(Intent(this, destino))
        finish()
    }
}

@Composable
fun SplashScreen() {
    var visible by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo_meditrack),
            contentDescription = "MediTrack Logo",
            modifier = Modifier
                .size(180.dp)
                .alpha(alpha)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nombre de la app
        Text(
            text = "MediTrack",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A237E),
            modifier = Modifier.alpha(alpha)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtítulo
        Text(
            text = "Tu salud, siempre a tiempo",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.alpha(alpha)
        )
    }
}