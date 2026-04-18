package com.example.meditrackservice.alarm

// Android core
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.meditrackservice.data.local.AlarmasProgramadasStore

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Tu modelo
import com.example.meditrackservice.data.model.AlarmaResponse


object AlarmScheduler {

    fun programarAlarmas(context: Context, alarmas: List<AlarmaResponse>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val idsNuevos = alarmas.map { it.id }
        val idsAnteriores = AlarmasProgramadasStore.obtenerIds(context)

        // ← Cancelar las que ya no existen o fueron modificadas
        val idsCancelar = idsAnteriores.filter { it !in idsNuevos }
        idsCancelar.forEach { id ->
            cancelarAlarma(context, id)
        }

        // ← Cancelar todas las actuales para reprogramar con datos frescos
        idsNuevos.forEach { id -> cancelarAlarma(context, id) }

        // ← Programar las nuevas
        alarmas.forEach { alarma ->
            programarUna(context, alarmManager, alarma)
        }

        // ← Guardar los IDs programados
        AlarmasProgramadasStore.guardarIds(context, idsNuevos)
    }

    private fun cancelarTodasLasAlarmas(context: Context, alarmaIds: List<Long>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmaIds.forEach { id ->
            cancelarAlarma(context, id)
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
            putExtra("forma_farmaceutica", alarma.formaFarmaceutica)
            putExtra("fecha_hora", alarma.fechaHora)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarma.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Sin permiso para alarmas exactas: ${e.message}")
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