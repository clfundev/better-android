package io.github.clfundev.better.data.network.api

import io.github.clfundev.better.data.network.model.ApiResponse
import io.github.clfundev.better.data.network.model.LoginRequest
import io.github.clfundev.better.data.network.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApi {
    @POST("/user/login")
    suspend fun login(@Body loginRequest: LoginRequest): ApiResponse<String>

    @POST("/user/register")
    suspend fun register(@Body registerRequest: RegisterRequest): ApiResponse<Unit>
}