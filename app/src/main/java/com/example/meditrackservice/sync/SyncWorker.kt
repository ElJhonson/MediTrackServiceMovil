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

class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val tokenDataStore = TokenDataStoreProvider.getInstance(context)
            val token = tokenDataStore.accessToken.first()

            if (token.isNullOrBlank()) return Result.failure()

            val apiService = RetrofitClient.create { token }
            val alarmas = apiService.obtenerAlarmasHoy(pacienteId = null)
            val pendientes = alarmas.filter { it.estado == "PENDIENTE" }

            AlarmScheduler.programarAlarmas(context, pendientes)

            Result.success()
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure() // Token expirado, no reintentar
                else -> Result.retry()  // Error de servidor, reintentar
            }
        } catch (e: Exception) {
            Result.retry() // Sin internet, reintentar
        }
    }
}