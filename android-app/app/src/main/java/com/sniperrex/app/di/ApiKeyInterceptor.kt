package com.sniperrex.app.di

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Menyisipkan kredensial user sebagai header — backend tidak menyimpan key. */
class ApiKeyInterceptor @Inject constructor(
    private val provider: ApiKeyProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        provider.currentTwelve()?.let { builder.header("X-Twelve-Key", it) }
        provider.currentAi()?.let { builder.header("X-AI-Key", it) }
        provider.currentAiBase()?.let { builder.header("X-AI-Base-Url", it) }
        provider.currentAiModel()?.let { builder.header("X-AI-Model", it) }
        return chain.proceed(builder.build())
    }
}
