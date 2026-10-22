package io.github.clfundev.better.data.network.retrofit

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import io.github.clfundev.better.BuildConfig
import io.github.clfundev.better.data.network.api.BackupApi
import io.github.clfundev.better.data.network.api.UserApi
import io.github.clfundev.better.data.pref.AppPreferences
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class RetrofitClient(appPreferences: AppPreferences) {
    private val retrofit = buildRetrofit(appPreferences)

    val userApi: UserApi = retrofit.create(UserApi::class.java)

    val backupApi: BackupApi = retrofit.create(BackupApi::class.java)

    private fun buildRetrofit(appPreferences: AppPreferences): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(RequestInterceptor(appPreferences))
            .build()
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // 让Gson支持LocalDateTime的序列化和反序列化
    private class LocalDateTimeAdapter :
        JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        override fun serialize(
            src: LocalDateTime,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement = JsonPrimitive(src.toString())

        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): LocalDateTime = LocalDateTime.parse(json.asString)
    }

    companion object {
        private const val CONNECT_TIMEOUT = 3L // s
        private const val READ_TIMEOUT = 3L // s

        @Volatile
        private var instance: RetrofitClient? = null

        fun getInstance(appPreferences: AppPreferences): RetrofitClient =
            instance ?: synchronized(this) {
                instance ?: RetrofitClient(appPreferences).also { instance = it }
            }
    }
}
