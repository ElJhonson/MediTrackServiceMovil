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
import com.example.meditrackservice.data.local.AccionesPendientesStore
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import com.example.meditrackservice.sync.RetryScheduler
import com.example.meditrackservice.ui.theme.MediTrackServiceTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// alarm/AlarmActivity.kt
class AlarmActivity : ComponentActivity() {

    private var alarmaId = -1L
    private var medicinaNombre = ""
    private var formaFarmaceutica = ""
    private var fechaHora = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        onBackPressedDispatcher.addCallback(this) { }

        cargarDatos(intent)
        mostrarPantalla()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Agregar a la cola en vez de reemplazar la pantalla actual
        val nuevaAlarma = AlarmQueue.AlarmaData(
            alarmaId = intent.getLongExtra("alarma_id", -1),
            medicinaNombre = intent.getStringExtra("medicina_nombre") ?: "Medicina",
            formaFarmaceutica = intent.getStringExtra("forma_farmaceutica") ?: "",
            fechaHora = intent.getStringExtra("fecha_hora") ?: ""
        )
        if (nuevaAlarma.alarmaId != -1L) {
            AlarmQueue.agregar(nuevaAlarma)
        }
    }

    private fun cargarDatos(intent: Intent) {
        alarmaId = intent.getLongExtra("alarma_id", -1)
        medicinaNombre = intent.getStringExtra("medicina_nombre") ?: "Medicina"
        formaFarmaceutica = intent.getStringExtra("forma_farmaceutica") ?: ""
        fechaHora = intent.getStringExtra("fecha_hora") ?: ""
    }

    private fun mostrarPantalla() {
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
                val refreshToken = TokenDataStoreProvider.getInstance(applicationContext)
                    .refreshToken.first()

                // ← con refresh automático
                val apiService = RetrofitClient.create(
                    tokenProvider = { token },           // ← token real
                    refreshTokenProvider = { refreshToken },
                    onTokenRefreshed = { nuevoToken ->
                        scope.launch {
                            TokenDataStoreProvider.getInstance(applicationContext)
                                .guardarTokens(nuevoToken, refreshToken ?: "")
                        }
                    },
                    onSessionExpired = {
                        Log.w("AlarmActivity", "Sesión expirada")
                    }
                )
                apiService.actualizarEstado(alarmaId, estado)
            } catch (e: Exception) {
            Log.w("AlarmActivity", "Sin internet, guardando acción pendiente")
            AccionesPendientesStore.guardar(applicationContext, alarmaId, estado)
            RetryScheduler.programar(applicationContext)
        }
        }

        val siguiente = AlarmQueue.siguiente()
        if (siguiente != null) {
            this.alarmaId = siguiente.alarmaId
            this.medicinaNombre = siguiente.medicinaNombre
            this.formaFarmaceutica = siguiente.formaFarmaceutica
            this.fechaHora = siguiente.fechaHora
            mostrarPantalla()
        } else {
            finish()
        }
    }
}