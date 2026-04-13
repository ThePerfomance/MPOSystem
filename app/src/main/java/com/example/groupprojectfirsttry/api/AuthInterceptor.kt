package com.example.groupprojectfirsttry.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        // Логируем URL запроса
        val url = request.url.toString()
        
        // Получаем токен через ApiClient динамически для каждого запроса
        val token = ApiClient.getTokenManager()?.getAccessToken()

        if (token != null) {
            Log.d("AuthInterceptor", "Adding token to request: $url")
            requestBuilder.addHeader("Authorization", "Bearer $token")
        } else {
            Log.w("AuthInterceptor", "No token found for request: $url")
        }

        val response = chain.proceed(requestBuilder.build())
        
        if (response.code == 401) {
            Log.e("AuthInterceptor", "Unauthorized (401) for: $url. Token might be expired.")
        }
        
        return response
    }
}
