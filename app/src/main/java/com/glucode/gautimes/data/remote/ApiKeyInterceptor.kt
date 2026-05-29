package com.glucode.gautimes.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ApiKeyInterceptor @Inject constructor(
    private val apiKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authenticatedRequest = if (
            apiKey.isNotBlank() &&
            request.url.encodedPath.startsWith(PROTECTED_PATH_PREFIX)
        ) {
            request.newBuilder()
                .header(API_KEY_HEADER, apiKey)
                .build()
        } else {
            request
        }

        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        const val API_KEY_HEADER = "X-Api-Key"
        const val PROTECTED_PATH_PREFIX = "/v1/"
    }
}
