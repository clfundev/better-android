package io.github.clfundev.better.data.network.model

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
)
