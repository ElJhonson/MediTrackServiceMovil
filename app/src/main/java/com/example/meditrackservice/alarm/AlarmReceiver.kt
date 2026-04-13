package com.example.meditrackservice.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat


class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmaId = intent.getLongExtra("alarma_id", -1)
        val medicinaNombre = intent.getStringExtra("medicina_nombre") ?: "Medicina"

        if (alarmaId == -1L) return

        mostrarNotificacion(context, alarmaId, medicinaNombre)
    }

    private fun mostrarNotificacion(
        context: Context,
        alarmaId: Long,
        medicinaNombre: String
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
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intentTomada = Intent(context, ActionReceiver::class.java).apply {
            putExtra("alarma_id", alarmaId)
            putExtra("estado", "TOMADA")
        }
        val pendingTomada = PendingIntent.getBroadcast(
            context,
            (alarmaId * 10).toInt(),
            intentTomada,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intentOmitida = Intent(context, ActionReceiver::class.java).apply {
            putExtra("alarma_id", alarmaId)
            putExtra("estado", "OMITIDA")
        }
        val pendingOmitida = PendingIntent.getBroadcast(
            context,
            (alarmaId * 10 + 1).toInt(),
            intentOmitida,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("💊 Hora de tu medicina")
            .setContentText(medicinaNombre)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(
                android.R.drawable.checkbox_on_background,
                "✅ Tomada",
                pendingTomada
            )
            .addAction(
                android.R.drawable.ic_delete,
                "❌ Omitida",
                pendingOmitida
            )
            .build()

        // ← FIX: verificar permiso antes de notify()
        val tienePermiso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12 o menor no necesita este permiso
        }

        if (tienePermiso) {
            NotificationManagerCompat.from(context).notify(alarmaId.toInt(), notificacion)
        } else {
            Log.w("AlarmReceiver", "Sin permiso para mostrar notificaciones")
        }
    }
}