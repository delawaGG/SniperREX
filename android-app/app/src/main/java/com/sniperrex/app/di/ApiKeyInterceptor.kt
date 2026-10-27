package com.sniperrex.app.di

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Menyisipkan kredensial user sebagai header dan mengarahkan request ke
 * base URL yang diisi user (bukan hardcode) — backend tidak menyimpan key.
 */
class ApiKeyInterceptor @Inject constructor(
    private val provider: ApiKeyProvider,
    private val backendUrl: BackendUrlProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
        provider.currentTwelve()?.let { builder.header("X-Twelve-Key", it) }
        provider.currentAi()?.let { builder.header("X-AI-Key", it) }
        provider.currentAiBase()?.let { builder.header("X-AI-Base-Url", it) }
        provider.currentAiModel()?.let { builder.header("X-AI-Model", it) }

        val base = backendUrl.current().toHttpUrlOrNull()
        if (base != null && original.url.host != base.host) {
            val rebuilt = original.url.newBuilder()
                .scheme(base.scheme)
                .host(base.host)
                .port(base.port)
                .build()
            builder.url(rebuilt)
        }
        return chain.proceed(builder.build())
    }
}
