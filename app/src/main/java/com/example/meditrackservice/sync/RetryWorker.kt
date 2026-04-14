package com.example.meditrackservice.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.AccionesPendientesStore
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import kotlinx.coroutines.flow.first

class RetryWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendientes = AccionesPendientesStore.obtenerTodas(context)
        if (pendientes.isEmpty()) return Result.success()

        val tokenDataStore = TokenDataStoreProvider.getInstance(context)
        val token = tokenDataStore.accessToken.first()
        val refreshToken = tokenDataStore.refreshToken.first()

        if (token.isNullOrBlank()) return Result.failure()

        val apiService = RetrofitClient.create(
            tokenProvider = { token },
            refreshTokenProvider = { refreshToken },
            onTokenRefreshed = { nuevoToken ->
                kotlinx.coroutines.runBlocking {
                    tokenDataStore.guardarTokens(nuevoToken, refreshToken ?: "")
                }
            },
            onSessionExpired = {}
        )

        var todasExitosas = true

        pendientes.forEach { accion ->
            try {
                apiService.actualizarEstado(accion.alarmaId, accion.estado)
                AccionesPendientesStore.eliminar(context, accion.alarmaId, accion.estado)
            } catch (e: Exception) {
                todasExitosas = false
            }
        }

        return if (todasExitosas) Result.success() else Result.retry()
    }
}