package com.example.meditrackservice.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.TokenDataStore
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val tokenDataStore = TokenDataStoreProvider.getInstance(context)
                val token = tokenDataStore.accessToken.first()

                if (token.isNullOrBlank()) return@launch

                val apiService = RetrofitClient.create { token }
                val alarmas = apiService.obtenerAlarmasHoy(pacienteId = null)
                val pendientes = alarmas.filter { it.estado == "PENDIENTE" }

                AlarmScheduler.programarAlarmas(context, pendientes)

            } catch (e: Exception) {
                Log.e("BootReceiver", "Error reprogramando alarmas: ${e.message}")
            }
        }
    }
}