package com.example.meditrackservice.alarm


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.AccionesPendientesStore
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import com.example.meditrackservice.sync.RetryScheduler
import kotlinx.coroutines.flow.first

class OmitirAlarmaWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val alarmaId = inputData.getLong("alarma_id", -1)
        if (alarmaId == -1L) return Result.failure()

        return try {
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

            apiService.actualizarEstado(alarmaId, "OMITIDA")
            Result.success()

        } catch (e: Exception) {
            // Sin internet → guardar pendiente
            AccionesPendientesStore.guardar(context, alarmaId, "OMITIDA")
            RetryScheduler.programar(context)
            Result.success()
        }
    }
}