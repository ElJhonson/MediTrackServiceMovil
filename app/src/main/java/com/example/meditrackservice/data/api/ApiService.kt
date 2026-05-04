package com.example.meditrackservice.data.api

import com.example.meditrackservice.data.model.AlarmaResponse
import com.example.meditrackservice.data.model.LoginRequest
import com.example.meditrackservice.data.model.LoginResponse
import com.example.meditrackservice.data.model.RefreshRequest
import com.example.meditrackservice.data.model.RefreshResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/acceder")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest
    ): RefreshResponse

    @GET("alarmas/hoy")
    suspend fun obtenerAlarmasHoy(
        @Query("pacienteId") pacienteId: Long? = null
    ): List<AlarmaResponse>

    @PATCH("alarmas/{id}/estado")
    suspend fun actualizarEstado(
        @Path("id") id: Long,
        @Query("estado") estado: String
    ): ResponseBody

    @GET("alarmas/historial")
    suspend fun obtenerHistorial(
        @Query("pacienteId") pacienteId: Long? = null,
        @Query("fechaInicio") fechaInicio: String? = null,
        @Query("fechaFin") fechaFin: String? = null
    ): List<AlarmaResponse>
}