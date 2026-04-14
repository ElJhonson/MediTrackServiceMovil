package com.example.meditrackservice.sync


import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object RetryScheduler {

    private const val RETRY_WORK_NAME = "retry_acciones_pendientes"

    fun programar(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val retryRequest = OneTimeWorkRequestBuilder<RetryWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            RETRY_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            retryRequest
        )
    }
}