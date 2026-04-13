package com.example.meditrackservice.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String
)

data class RefreshRequest(
    @SerializedName("refreshToken")
    val refreshToken: String
)

data class RefreshResponse(
    @SerializedName("accessToken")
    val accessToken: String
)