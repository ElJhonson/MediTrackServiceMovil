package com.example.meditrackservice.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service                    // ← import correcto
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat

// alarm/AlarmForegroundService.kt
class AlarmForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val channelId = "servicio_alarmas"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servicio de alarmas",
                NotificationManager.IMPORTANCE_LOW // ← LOW para no molestar
            ).apply {
                description = "Mantiene las alarmas activas"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificacion = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("MediTrack activo")
            .setContentText("Monitoreando tus alarmas de medicina")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // ← no se puede quitar
            .setSilent(true)  // ← sin sonido
            .build()

        startForeground(1000, notificacion)

        return START_STICKY // ← se reinicia si el sistema lo mata
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        // ← Cuando el usuario cierra la app desde multitarea
        // el servicio intenta reiniciarse
        val restartIntent = Intent(applicationContext, AlarmForegroundService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }
}