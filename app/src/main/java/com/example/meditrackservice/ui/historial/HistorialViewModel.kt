package com.example.meditrackservice.ui.historial

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import com.example.meditrackservice.data.model.AlarmaResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class HistorialViewModel(private val context: Application) : AndroidViewModel(context) {

    private val tokenDataStore = TokenDataStoreProvider.getInstance(context)

    private val _alarmas = MutableLiveData<List<AlarmaResponse>>()
    val alarmas: LiveData<List<AlarmaResponse>> = _alarmas

    private val _estado = MutableLiveData<HistorialEstado>()
    val estado: LiveData<HistorialEstado> = _estado

    private val _semanaOffset = MutableLiveData(0)
    val semanaOffset: LiveData<Int> = _semanaOffset

    private val _rangoSemana = MutableLiveData<Pair<LocalDate, LocalDate>>()
    val rangoSemana: LiveData<Pair<LocalDate, LocalDate>> = _rangoSemana

    private var loadJob: Job? = null

    init {
        calcularRango(0)
        cargarHistorial()
    }

    private fun calcularRango(offset: Int) {
        val hoy = LocalDate.now()
        val lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(offset.toLong())
        val domingo = lunes.plusDays(6)
        _rangoSemana.value = Pair(lunes, domingo)
    }

    fun semanaAnterior() {
        val nuevo = (_semanaOffset.value ?: 0) - 1
        _semanaOffset.value = nuevo
        calcularRango(nuevo)
        cargarHistorial()
    }

    fun semanaSiguiente() {
        val nuevo = (_semanaOffset.value ?: 0) + 1
        _semanaOffset.value = nuevo
        calcularRango(nuevo)
        cargarHistorial()
    }

    fun cargarHistorial() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _estado.value = HistorialEstado.Cargando

            try {
                val token = tokenDataStore.accessToken.first()
                val refreshToken = tokenDataStore.refreshToken.first()

                if (token.isNullOrBlank()) {
                    _estado.value = HistorialEstado.Error("Sesión expirada")
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
                        _estado.postValue(HistorialEstado.Error("Sesión expirada"))
                    }
                )

                val rango = _rangoSemana.value ?: return@launch
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE

                val resultado = apiService.obtenerHistorial(
                    pacienteId = null,
                    fechaInicio = rango.first.format(formatter),
                    fechaFin = rango.second.format(formatter)
                )

                _alarmas.value = resultado
                _estado.value = HistorialEstado.Exitoso

            } catch (e: Exception) {
                _estado.value = HistorialEstado.Error("Sin conexión a internet")
            }
        }
    }
}

sealed class HistorialEstado {
    object Cargando : HistorialEstado()
    object Exitoso : HistorialEstado()
    data class Error(val mensaje: String) : HistorialEstado()
}