package com.project.medi_agent.data.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * Unified Chat Service for OpenAI compatible APIs (DeepSeek, Zhipu GLM-4, etc.)
 */
interface ChatService {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    @Streaming
    fun chatCompletionStream(@Body request: ChatCompletionRequest): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatCompletionRequest): ChatCompletionResponse
}
