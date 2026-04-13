package com.example.meditrackservice.ui.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meditrackservice.data.api.ApiService
import com.example.meditrackservice.data.api.RetrofitClient
import com.example.meditrackservice.data.local.TokenDataStore
import com.example.meditrackservice.data.local.TokenDataStoreProvider
import com.example.meditrackservice.data.model.LoginRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException

// ui/login/LoginViewModel.kt
class LoginViewModel(private val context: Application) : AndroidViewModel(context) {

    private val tokenDataStore = TokenDataStoreProvider.getInstance(context)
    private lateinit var apiService: ApiService

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    init {
        // Inicializar Retrofit sin token (login no lo necesita)
        apiService = RetrofitClient.create { null }
    }

    fun login(telefono: String, contrasena: String) {

        if (telefono.isBlank() || contrasena.isBlank()) {
            _loginState.value = LoginState.Error("Completa todos los campos")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            try {
                val response = apiService.login(LoginRequest(telefono, contrasena))
                tokenDataStore.guardarTokens(response.accessToken, response.refreshToken)
                _loginState.value = LoginState.Success

            } catch (e: HttpException) {
                val mensaje = when (e.code()) {
                    401 -> "Teléfono o contraseña incorrectos"
                    else -> "Error del servidor: ${e.code()}"
                }
                _loginState.value = LoginState.Error(mensaje)

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error: ${e::class.simpleName} - ${e.message}")

                _loginState.value = LoginState.Error("Sin conexión a internet")
            }
        }
    }
}

// Estados posibles del login
sealed class LoginState {
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val mensaje: String) : LoginState()
}