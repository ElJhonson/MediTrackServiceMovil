package com.example.meditrackservice.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meditrackservice.alarm.AlarmQueue
import com.example.meditrackservice.alarm.AlarmScheduler
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.AccionesPendientesStore
import com.example.meditrackservice.data.local.AlarmasProgramadasStore
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import com.example.meditrackservice.data.model.AlarmaResponse
import com.example.meditrackservice.data.model.RefreshRequest
import com.example.meditrackservice.sync.RetryScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                val token = tokenDataStore.accessToken.first()
                val refreshToken = tokenDataStore.refreshToken.first()

                if (token.isNullOrBlank()) {
                    _estado.value = AlarmaEstado.SesionExpirada
                    return@launch
                }

                val apiService = RetrofitClient.create(
                    tokenProvider = { token },
                    refreshTokenProvider = { refreshToken },
                    onTokenRefreshed = { nuevoToken ->
                        viewModelScope.launch {
                            tokenDataStore.guardarTokens(nuevoToken, refreshToken ?: "")
                        }
                    },
                    onSessionExpired = {
                        _estado.postValue(AlarmaEstado.SesionExpirada)
                    }
                )

                val resultado = apiService.obtenerAlarmasHoy(pacienteId = null)

                // ← Marcar como OMITIDA con 5 minutos de gracia
                val ahora = LocalDateTime.now(ZoneId.of("America/Mexico_City"))
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

                resultado.filter { alarma ->
                    alarma.estado == "PENDIENTE" &&
                            LocalDateTime.parse(alarma.fechaHora, formatter)
                                .plusMinutes(5)
                                .isBefore(ahora)
                }.forEach { alarma ->
                    try {
                        apiService.actualizarEstado(alarma.id, "OMITIDA")
                    } catch (e: Exception) {
                        AccionesPendientesStore.guardar(context, alarma.id, "OMITIDA")
                        RetryScheduler.programar(context)
                    }
                }

                // ← Recargar después de actualizar las omitidas
                val actualizado = apiService.obtenerAlarmasHoy(pacienteId = null)
                val pendientes = actualizado.filter { it.estado == "PENDIENTE" }

                _alarmas.value = actualizado
                _estado.value = AlarmaEstado.Exitoso

                AlarmScheduler.programarAlarmas(context, pendientes)

            } catch (e: HttpException) {
                _estado.postValue(AlarmaEstado.Error("Error del servidor: ${e.code()}"))
            } catch (e: Exception) {
                _estado.postValue(AlarmaEstado.Error("Sin conexión a internet"))
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            // Limpiar tokens
            tokenDataStore.limpiarTokens()
            // Cancelar alarmas programadas
            AlarmasProgramadasStore.limpiar(context)
            // Limpiar cola
            AlarmQueue.limpiar()
            // Notificar a la UI
            _estado.postValue(AlarmaEstado.SesionExpirada)
        }
    }

    private suspend fun intentarRefresh(): Boolean {
        return try {
            val refreshToken = tokenDataStore.refreshToken.first() ?: return false

            // ← pasar todos los parámetros requeridos
            val tempApi = RetrofitClient.create(
                tokenProvider = { null },
                refreshTokenProvider = { null },
                onTokenRefreshed = {},
                onSessionExpired = {}
            )

            val response = tempApi.refresh(RefreshRequest(refreshToken))
            tokenDataStore.guardarTokens(response.accessToken, refreshToken)
            true
        } catch (e: Exception) {
            false
        }
    }

    private var syncJob: Job? = null

    fun iniciarSyncEnTiempoReal() {
        syncJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                cargarAlarmasHoy()
            }
        }
    }

    fun detenerSyncEnTiempoReal() {
        syncJob?.cancel()
        syncJob = null
    }
}

sealed class AlarmaEstado {
    object Cargando : AlarmaEstado()
    object Exitoso : AlarmaEstado()
    object SesionExpirada : AlarmaEstado()
    data class Error(val mensaje: String) : AlarmaEstado()
}