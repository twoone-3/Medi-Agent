package com.project.medi_agent.data.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val gson: Gson = GsonBuilder().create()

    private fun createOkHttpClient(apiKey: String? = null): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS // Avoid reading body for streaming
        }

        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // Longer timeout for AI streaming
            .addInterceptor(logging)

        if (!apiKey.isNullOrBlank()) {
            val headerInterceptor = Interceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(req)
            }
            builder.addInterceptor(headerInterceptor)
        }

        return builder.build()
    }

    fun <T> createService(baseUrl: String, serviceClass: Class<T>, apiKey: String? = null): T {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient(apiKey))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(serviceClass)
    }
}
