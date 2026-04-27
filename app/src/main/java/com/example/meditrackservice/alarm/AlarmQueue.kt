package com.example.meditrackservice.alarm

// alarm/AlarmQueue.kt
object AlarmQueue {

    data class AlarmaData(
        val alarmaId: Long,
        val medicinaNombre: String,
        val formaFarmaceutica: String,
        val fechaHora: String
    )

    private val cola = mutableListOf<AlarmaData>()

    fun agregar(alarma: AlarmaData) {
        synchronized(this) {
            // ← No agregar si ya existe en la cola
            if (cola.none { it.alarmaId == alarma.alarmaId }) {
                cola.add(alarma)
            }
        }
    }

    fun siguiente(): AlarmaData? {
        return synchronized(this) {
            if (cola.isNotEmpty()) cola.removeAt(0) else null
        }
    }

    fun hayPendientes(): Boolean {
        return synchronized(this) { cola.isNotEmpty() }
    }

    // ← función nueva
    fun contiene(alarmaId: Long): Boolean {
        return synchronized(this) { cola.any { it.alarmaId == alarmaId } }
    }

    fun limpiar() {
        synchronized(this) { cola.clear() }
    }
}