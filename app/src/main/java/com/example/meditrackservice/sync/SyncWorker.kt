package com.example.meditrackservice.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.meditrackservice.alarm.AlarmScheduler
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.TokenDataStore
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

// sync/SyncWorker.kt
class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val tokenDataStore = TokenDataStoreProvider.getInstance(context)
            val token = tokenDataStore.accessToken.first()
            val refreshToken = tokenDataStore.refreshToken.first() // ← leer aquí

            if (token.isNullOrBlank()) return Result.failure()

            val apiService = RetrofitClient.create(
                tokenProvider = { token },
                refreshTokenProvider = { refreshToken },
                onTokenRefreshed = { nuevoToken ->
                    // ← usar kotlinx.coroutines.runBlocking para llamar suspend
                    kotlinx.coroutines.runBlocking {
                        tokenDataStore.guardarTokens(nuevoToken, refreshToken ?: "")
                    }
                },
                onSessionExpired = {}
            )

            val alarmas = apiService.obtenerAlarmasHoy(pacienteId = null)
            val pendientes = alarmas.filter { it.estado == "PENDIENTE" }

            AlarmScheduler.programarAlarmas(context, pendientes)

            Result.success()
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure()
                else -> Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}