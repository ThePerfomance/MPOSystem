package com.example.groupprojectfirsttry.api

import android.content.Context
import android.util.Log
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    const val BASE_URL = "http://192.168.31.96:8000/"
    
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
        // Создаем стандартный логгер
        val logger = HttpLoggingInterceptor { message ->
            Log.d("TrainingNetwork", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Создаем фильтрующий перехватчик
        val trainingFilterInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()
            
            // Логируем только если URL содержит ключевые слова тренажера
            if (url.contains("training") || url.contains("test-results") || url.contains("user-answers")) {
                logger.intercept(chain)
            } else {
                chain.proceed(request)
            }
        }

        OkHttpClient.Builder()
            .addInterceptor(trainingFilterInterceptor)
            .addInterceptor(AuthInterceptor())
            .authenticator(TokenAuthenticator())
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

class TokenAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val tm = ApiClient.getTokenManager() ?: return null
        val refreshToken = tm.getRefreshToken() ?: return null

        synchronized(this) {
            val currentToken = tm.getAccessToken()
            val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")
            
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            Log.d("TokenAuthenticator", "Attempting to refresh access token...")

            return try {
                val refreshRetrofit = Retrofit.Builder()
                    .baseUrl(ApiClient.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                
                val refreshService = refreshRetrofit.create(ApiService::class.java)

                val refreshResponse = kotlinx.coroutines.runBlocking {
                    refreshService.refreshToken(mapOf("refresh" to refreshToken))
                }

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val newTokens = refreshResponse.body()!!
                    Log.d("TokenAuthenticator", "Token refresh successful!")
                    tm.saveTokens(newTokens.access, newTokens.refresh)
                    
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.access}")
                        .build()
                } else {
                    Log.e("TokenAuthenticator", "Refresh failed: ${refreshResponse.code()}")
                    tm.clear()
                    null
                }
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Refresh exception: ${e.message}")
                null
            }
        }
    }
}
