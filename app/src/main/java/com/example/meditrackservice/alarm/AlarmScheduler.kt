package com.example.meditrackservice.alarm

// Android core
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Tu modelo
import com.example.meditrackservice.data.model.AlarmaResponse


object AlarmScheduler {

    fun programarAlarmas(context: Context, alarmas: List<AlarmaResponse>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmas.forEach { alarma ->
            programarUna(context, alarmManager, alarma)
        }
    }

    fun programarUna(context: Context, alarmManager: AlarmManager, alarma: AlarmaResponse) {

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val fechaLocal = LocalDateTime.parse(alarma.fechaHora, formatter)
        val zonaHoraria = ZoneId.of("America/Mexico_City")
        val triggerMillis = fechaLocal
            .atZone(zonaHoraria)
            .toInstant()
            .toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarma_id", alarma.id)
            putExtra("medicina_nombre", alarma.medicinaNombre)
            putExtra("fecha_hora", alarma.fechaHora)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarma.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ← FIX: verificar permiso antes de programar
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    // Sin permiso → usar setAndAllowWhileIdle (menos exacto pero funciona)
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                // Android 11 o menor → sin restricción
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Sin permiso para alarmas exactas: ${e.message}")
            // Fallback sin exactitud
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun cancelarAlarma(context: Context, alarmaId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmaId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}