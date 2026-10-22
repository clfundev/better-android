package io.github.clfundev.better.data.network.retrofit

import io.github.clfundev.better.data.pref.AppPreferences
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 请求拦截器，用于给请求头添加token
 */
class RequestInterceptor(private val appPreferences: AppPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        appPreferences.token?.let {
            builder.addHeader("token", it)
        }
        return chain.proceed(builder.build())
    }
}
