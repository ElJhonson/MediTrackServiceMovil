package com.example.meditrackservice.data.local

import android.content.Context

object AccionesPendientesStore {

    private const val PREFS_NAME = "acciones_pendientes"

    data class AccionPendiente(
        val alarmaId: Long,
        val estado: String
    )

    fun guardar(context: Context, alarmaId: Long, estado: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet("acciones", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        set.add("$alarmaId:$estado")
        prefs.edit().putStringSet("acciones", set).apply()
    }

    fun obtenerTodas(context: Context): List<AccionPendiente> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet("acciones", emptySet()) ?: emptySet()
        return set.mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                AccionPendiente(parts[0].toLong(), parts[1])
            } else null
        }
    }

    fun eliminar(context: Context, alarmaId: Long, estado: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet("acciones", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        set.remove("$alarmaId:$estado")
        prefs.edit().putStringSet("acciones", set).apply()
    }
}