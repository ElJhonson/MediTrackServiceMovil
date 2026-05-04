package com.example.meditrackservice.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.AlarmasProgramadasStore
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmForegroundService : Service() {

    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val channelId = "servicio_alarmas"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servicio de alarmas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene las alarmas activas"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificacion = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("MediTrack activo")
            .setContentText("Monitoreando tus alarmas de medicina")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(1000, notificacion)

        // ← Iniciar sync cada 5 minutos
        iniciarSyncPeriodico()

        return START_STICKY
    }

    private fun iniciarSyncPeriodico() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (true) {
                sincronizarAlarmas()
                delay(5 * 60 * 1000L) // ← 5 minutos
            }
        }
    }

    private suspend fun sincronizarAlarmas() {
        try {
            val tokenDataStore = TokenDataStoreProvider.getInstance(applicationContext)
            val token = tokenDataStore.accessToken.first()
            val refreshToken = tokenDataStore.refreshToken.first()

            if (token.isNullOrBlank()) return

            val apiService = RetrofitClient.create(
                tokenProvider = { token },
                refreshTokenProvider = { refreshToken },
                onTokenRefreshed = { nuevoToken ->
                    scope.launch {
                        tokenDataStore.guardarTokens(nuevoToken, refreshToken ?: "")
                    }
                },
                onSessionExpired = {}
            )

            val alarmas = apiService.obtenerAlarmasHoy(pacienteId = null)
            val pendientes = alarmas.filter { it.estado == "PENDIENTE" }

            AlarmScheduler.programarAlarmas(applicationContext, pendientes)

            Log.d("AlarmForegroundService", "Sync exitoso: ${pendientes.size} alarmas pendientes")

        } catch (e: Exception) {
            // Sin internet o error → no hacer nada, reintentar en 5 min
            Log.w("AlarmForegroundService", "Sync falló: ${e.message}")
        }
    }

    override fun onDestroy() {
        syncJob?.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, AlarmForegroundService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}