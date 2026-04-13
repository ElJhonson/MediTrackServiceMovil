package com.example.meditrackservice.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meditrackservice.alarm.AlarmScheduler
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import com.example.meditrackservice.data.model.AlarmaResponse
import com.example.meditrackservice.data.model.RefreshRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AlarmaViewModel(private val context: Application) : AndroidViewModel(context) {

    private val tokenDataStore = TokenDataStoreProvider.getInstance(context)

    private val _alarmas = MutableLiveData<List<AlarmaResponse>>()
    val alarmas: LiveData<List<AlarmaResponse>> = _alarmas

    private val _estado = MutableLiveData<AlarmaEstado>()
    val estado: LiveData<AlarmaEstado> = _estado

    fun cargarAlarmasHoy() {
        viewModelScope.launch {
            _estado.value = AlarmaEstado.Cargando

            try {
                // ← Leer el token AQUÍ, justo antes de usarlo
                val token = tokenDataStore.accessToken.first()

                if (token.isNullOrBlank()) {
                    _estado.value = AlarmaEstado.SesionExpirada
                    return@launch
                }

                val apiService = RetrofitClient.create { token }
                val resultado = apiService.obtenerAlarmasHoy(pacienteId = null)
                val pendientes = resultado.filter { it.estado == "PENDIENTE" }

                _alarmas.value = resultado
                _estado.value = AlarmaEstado.Exitoso

                AlarmScheduler.programarAlarmas(context, pendientes)

            } catch (e: HttpException) {
                when (e.code()) {
                    401, 403 -> {
                        val refreshExitoso = intentarRefresh()
                        if (refreshExitoso) cargarAlarmasHoy()
                        else _estado.value = AlarmaEstado.SesionExpirada
                    }
                    else -> _estado.value =
                        AlarmaEstado.Error("Error del servidor: ${e.code()}")
                }
            } catch (e: Exception) {
                _estado.value = AlarmaEstado.Error("Sin conexión a internet")
            }
        }
    }

    private suspend fun intentarRefresh(): Boolean {
        return try {
            val refreshToken = tokenDataStore.refreshToken.first() ?: return false
            val tempApi = RetrofitClient.create { null }
            val response = tempApi.refresh(RefreshRequest(refreshToken))
            tokenDataStore.guardarTokens(response.accessToken, refreshToken)
            true
        } catch (e: Exception) {
            false
        }
    }
}

sealed class AlarmaEstado {
    object Cargando : AlarmaEstado()
    object Exitoso : AlarmaEstado()
    object SesionExpirada : AlarmaEstado()
    data class Error(val mensaje: String) : AlarmaEstado()
}