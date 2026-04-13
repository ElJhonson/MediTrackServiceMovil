package com.example.meditrackservice.data.model

data class AlarmaResponse(
    val id: Long,
    val alarmaConfigId: Long,
    val medicinaId: Long,
    val medicinaNombre: String,
    val fechaHora: String,  // "2025-04-10T08:00:00"
    val estado: String,     // "PENDIENTE", "TOMADA", "OMITIDA"
    val notificada: Boolean
)
