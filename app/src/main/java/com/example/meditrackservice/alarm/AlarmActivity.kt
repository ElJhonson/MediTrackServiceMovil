package com.example.meditrackservice.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import com.example.meditrackservice.ui.theme.MediTrackServiceTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mostrar sobre la pantalla de bloqueo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // ← reemplaza onBackPressed, bloquea el botón atrás
        onBackPressedDispatcher.addCallback(this) {
            // No hacer nada → fuerza al usuario a elegir una opción
        }

        startService(Intent(this, AlarmSoundService::class.java))

        val alarmaId = intent.getLongExtra("alarma_id", -1)
        val medicinaNombre = intent.getStringExtra("medicina_nombre") ?: "Medicina"
        val formaFarmaceutica = intent.getStringExtra("forma_farmaceutica") ?: ""
        val fechaHora = intent.getStringExtra("fecha_hora") ?: ""

        val hora = if (fechaHora.length >= 16) fechaHora.substring(11, 16) else ""

        setContent {
            MediTrackServiceTheme {
                AlarmScreen(
                    hora = hora,
                    medicinaNombre = medicinaNombre,
                    formaFarmaceutica = formaFarmaceutica,
                    onAccion = { estado ->
                        registrarAccion(alarmaId, estado)
                    }
                )
            }
        }
    }

    // alarm/AlarmActivity.kt
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Si llega una nueva alarma mientras esta pantalla está abierta
        // detener sonido anterior y cargar los datos de la nueva
        stopService(Intent(this, AlarmSoundService::class.java))
        setIntent(intent)

        val alarmaId = intent.getLongExtra("alarma_id", -1)
        val medicinaNombre = intent.getStringExtra("medicina_nombre") ?: "Medicina"
        val formaFarmaceutica = intent.getStringExtra("forma_farmaceutica") ?: ""
        val fechaHora = intent.getStringExtra("fecha_hora") ?: ""
        val hora = if (fechaHora.length >= 16) fechaHora.substring(11, 16) else ""

        startService(Intent(this, AlarmSoundService::class.java))

        setContent {
            MediTrackServiceTheme {
                AlarmScreen(
                    hora = hora,
                    medicinaNombre = medicinaNombre,
                    formaFarmaceutica = formaFarmaceutica,
                    onAccion = { estado -> registrarAccion(alarmaId, estado) }
                )
            }
        }
    }

    private fun registrarAccion(alarmaId: Long, estado: String) {
        stopService(Intent(this, AlarmSoundService::class.java))

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val token = TokenDataStoreProvider.getInstance(applicationContext)
                    .accessToken.first()
                val apiService = RetrofitClient.create { token }
                apiService.actualizarEstado(alarmaId, estado)
            } catch (e: Exception) {
                Log.e("AlarmActivity", "Error actualizando estado: ${e.message}")
            }
        }

        finish()
    }
}