package com.example.meditrackservice.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
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

        lanzarAlarma(context, alarmaId, medicinaNombre, formaFarmaceutica, fechaHora)

        wakeLock.release()
    }

    // alarm/AlarmReceiver.kt — reemplazar el startActivity por esto
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
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Intent para abrir AlarmActivity al tocar la notificación
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

        // Notificación con fullScreenIntent → abre AlarmActivity en pantalla bloqueada
        val notificacion = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("💊 Hora de tu medicina")
            .setContentText(medicinaNombre)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true) // ← no se puede deslizar para quitar
            .setFullScreenIntent(pendingFullScreen, true) // ← abre AlarmActivity
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
    }
}