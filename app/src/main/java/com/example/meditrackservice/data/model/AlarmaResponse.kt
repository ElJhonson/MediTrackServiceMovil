package com.example.meditrackservice.data.model

import com.google.gson.annotations.SerializedName

data class AlarmaResponse(
    val id: Long,
    val alarmaConfigId: Long,
    val medicinaId: Long,
    val medicinaNombre: String,
    @SerializedName("dosageForm")
    val formaFarmaceutica: String,
    val fechaHora: String,  // "2025-04-10T08:00:00"
    val estado: String,     // "PENDIENTE", "TOMADA", "OMITIDA"
    val notificada: Boolean
)
