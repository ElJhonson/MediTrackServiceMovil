package com.example.meditrackservice.data.local


import android.content.Context

object AlarmasProgramadasStore {

    private const val PREFS_NAME = "alarmas_programadas"
    private const val KEY_IDS = "ids_programados"

    fun guardarIds(context: Context, ids: List<Long>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = ids.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_IDS, set).apply()
    }

    fun obtenerIds(context: Context): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }
    }

    fun limpiar(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}