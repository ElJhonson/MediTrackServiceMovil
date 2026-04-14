package com.example.meditrackservice.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.TokenDataStore
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// alarm/ActionReceiver.kt
class ActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmaId = intent.getLongExtra("alarma_id", -1)
        val estado = intent.getStringExtra("estado") ?: return

        if (alarmaId == -1L) return

        // Llamar a la API en una coroutine
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val tokenDataStore = TokenDataStoreProvider.getInstance(context)
                val token = tokenDataStore.accessToken.first()
                val apiService = RetrofitClient.create(
                    tokenProvider = { null },
                    refreshTokenProvider = { null },
                    onTokenRefreshed = {},
                    onSessionExpired = {}
                )

                apiService.actualizarEstado(alarmaId, estado)

            } catch (e: Exception) {
                // Si falla, guardar localmente para reintentar después
                Log.e("ActionReceiver", "Error actualizando estado: ${e.message}")
            }
        }

        // Cancelar la notificación inmediatamente al presionar
        NotificationManagerCompat.from(context)
            .cancel(alarmaId.toInt())
    }
}