package com.example.groupprojectfirsttry.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"
    
    @Volatile
    private var tokenManager: TokenManager? = null

    fun init(context: Context) {
        if (tokenManager == null) {
            synchronized(this) {
                if (tokenManager == null) {
                    tokenManager = TokenManager(context.applicationContext)
                }
            }
        }
    }

    fun getTokenManager(): TokenManager? = tokenManager

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
