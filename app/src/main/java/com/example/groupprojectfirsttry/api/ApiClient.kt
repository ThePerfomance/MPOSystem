package com.example.groupprojectfirsttry.api

import android.content.Context
import android.util.Log
import okhttp3.Authenticator
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "http://10.68.204.107:7600/"
    
    @Volatile
    private var tokenManager: TokenManager? = null
    
    private var cache: Cache? = null

    fun init(context: Context) {
        if (tokenManager == null) {
            synchronized(this) {
                if (tokenManager == null) {
                    tokenManager = TokenManager(context.applicationContext)
                    
                    // Инициализация кэша (10 МБ)
                    val cacheSize = 10L * 1024 * 1024
                    val cacheDir = File(context.cacheDir, "http_cache")
                    cache = Cache(cacheDir, cacheSize)
                }
            }
        }
    }

    fun getTokenManager(): TokenManager? = tokenManager

    private val okHttpClient: OkHttpClient by lazy {
        // Создаем стандартный логгер с уровнем HEADERS для экономии ресурсов
        val logger = HttpLoggingInterceptor { message ->
            Log.d("TrainingNetwork", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.HEADERS
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
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(trainingFilterInterceptor)
            .addInterceptor(AuthInterceptor())
            .authenticator(TokenAuthenticator())
            .cache(cache)
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

    // Оптимизированный сервис для обновления токена (без лишних перехватов и логов)
    val refreshService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
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
                val refreshResponse = kotlinx.coroutines.runBlocking {
                    ApiClient.refreshService.refreshToken(mapOf("refresh" to refreshToken))
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
