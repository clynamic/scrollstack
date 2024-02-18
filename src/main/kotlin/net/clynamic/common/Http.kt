package net.clynamic.common

import io.ktor.http.RequestConnectionPoint
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class HttpErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            throw IOException("Http Request Failed ${response.code}: ${response.message.let { it.ifEmpty { "No message" } }}")
        }

        return response
    }
}

class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}

/**
 * Get the base server URL from a [RequestConnectionPoint]
 *
 * This will fail if the server is behind a sub-path.
 */
val RequestConnectionPoint.serverUrl: String
    get() = HttpUrl.Builder()
        .scheme(
            when (serverPort) {
                443 -> "https"
                else -> "http"
            }
        )
        .host(serverHost)
        .build()
        .toString()