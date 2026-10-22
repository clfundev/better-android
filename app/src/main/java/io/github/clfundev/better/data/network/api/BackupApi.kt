package io.github.clfundev.better.data.network.api

import io.github.clfundev.better.data.network.model.ApiResponse
import io.github.clfundev.better.data.network.model.Backup
import io.github.clfundev.better.data.network.model.Page
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface BackupApi {
    @GET("/backup/page")
    suspend fun getBackups(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): ApiResponse<Page<Backup>>

    @Streaming
    @GET("/backup/{id}")
    suspend fun downloadBackup(@Path("id") id: String): Response<ResponseBody>

    @Multipart
    @POST("/backup/upload")
    suspend fun upload(@Part file: MultipartBody.Part): ApiResponse<Unit>
}