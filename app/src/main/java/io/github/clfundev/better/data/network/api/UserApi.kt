package io.github.clfundev.better.data.network.api

import io.github.clfundev.better.data.network.model.ApiResponse
import io.github.clfundev.better.data.network.model.LoginRequest
import io.github.clfundev.better.data.network.model.LoginResponse
import io.github.clfundev.better.data.network.model.RefreshRequest
import io.github.clfundev.better.data.network.model.RefreshResponse
import io.github.clfundev.better.data.network.model.RegisterRequest
import io.github.clfundev.better.data.network.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UserApi {
    @POST("/user/login")
    suspend fun login(@Body loginRequest: LoginRequest): ApiResponse<LoginResponse>

    @POST("/user/register")
    suspend fun register(@Body registerRequest: RegisterRequest): ApiResponse<Unit>

    @POST("/user/refresh")
    suspend fun refresh(@Body refreshRequest: RefreshRequest): ApiResponse<RefreshResponse>

    @POST("/user/logout")
    suspend fun logout(): ApiResponse<Unit>

    @GET("/user/info")
    suspend fun getUserInfo(): ApiResponse<User>
}