package com.example.meditrackservice.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
// alarm/AlarmReceiver.kt
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmaId = intent.getLongExtra("alarma_id", -1)
        val medicinaNombre = intent.getStringExtra("medicina_nombre") ?: "Medicina"
        val formaFarmaceutica = intent.getStringExtra("forma_farmaceutica") ?: ""
        val fechaHora = intent.getStringExtra("fecha_hora") ?: ""

        if (alarmaId == -1L) return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MediTrack::AlarmWakeLock"
        )
        wakeLock.acquire(10_000L)

        // ← llamar a lanzarAlarma, no startActivity directamente
        lanzarAlarma(context, alarmaId, medicinaNombre, formaFarmaceutica, fechaHora)

        wakeLock.release()
    }

    // ← mover lanzarAlarma DENTRO de la clase
    private fun lanzarAlarma(
        context: Context,
        alarmaId: Long,
        medicinaNombre: String,
        formaFarmaceutica: String,
        fechaHora: String
    ) {
        val channelId = "alarmas_medicinas"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de medicina",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarmas para tomar medicinas"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)       // ← salta el modo No Molestar
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("alarma_id", alarmaId)
            putExtra("medicina_nombre", medicinaNombre)
            putExtra("forma_farmaceutica", formaFarmaceutica)
            putExtra("fecha_hora", fechaHora)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }

        val pendingFullScreen = PendingIntent.getActivity(
            context,
            alarmaId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("💊 Hora de tu medicina")
            .setContentText("Toca para registrar: $medicinaNombre")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(pendingFullScreen, true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) // ← sonido
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500)) // ← vibración intensa
            .build()

        val tienePermiso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (tienePermiso) {
            NotificationManagerCompat.from(context).notify(alarmaId.toInt(), notificacion)
        }
        programarOmisionAutomatica(context, alarmaId)

    }

    // alarm/AlarmReceiver.kt — agregar al final de lanzarAlarma()
    private fun programarOmisionAutomatica(context: Context, alarmaId: Long) {
        val inputData = androidx.work.Data.Builder()
            .putLong("alarma_id", alarmaId)
            .build()

        // Ejecutar después de 5 minutos + 30 segundos de margen
        val omitirRequest = androidx.work.OneTimeWorkRequestBuilder<OmitirAlarmaWorker>()
            .setInputData(inputData)
            .setInitialDelay(5, java.util.concurrent.TimeUnit.MINUTES)
            .build()

        androidx.work.WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "omitir_alarma_$alarmaId",
                androidx.work.ExistingWorkPolicy.KEEP,
                omitirRequest
            )
    }
}