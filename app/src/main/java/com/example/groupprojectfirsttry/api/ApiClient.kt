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
    const val BASE_URL = "http://10.0.2.2:7600/"
    
    @Volatile
    private var tokenManager: TokenManager? = null
    private var appContext: Context? = null
    private var cache: Cache? = null

    fun init(context: Context) {
        if (appContext == null) {
            synchronized(this) {
                if (appContext == null) {
                    appContext = context.applicationContext
                    tokenManager = TokenManager(appContext!!)
                    
                    val cacheSize = 10L * 1024 * 1024
                    val cacheDir = File(appContext!!.cacheDir, "http_cache")
                    cache = Cache(cacheDir, cacheSize)
                }
            }
        }
    }

    fun getTokenManager(): TokenManager? = tokenManager

    // Интерцептор для создания искусственной задержки (чтобы видеть шиммеры)
    private val delayInterceptor = Interceptor { chain ->
        try {
            Thread.sleep(0) // Задержка 2 секунды
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        chain.proceed(chain.request())
    }

    private val okHttpClient: OkHttpClient by lazy {
        val context = appContext ?: throw IllegalStateException("ApiClient must be initialized with context before use")
        
        // Уровень BASIC вместо BODY: только метод, URL и статус ответа
        val logger = HttpLoggingInterceptor { message ->
            Log.d("NetworkLog", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(NetworkConnectivityInterceptor(context)) 
            .addInterceptor(delayInterceptor) // Добавляем задержку
            .addInterceptor(logger)
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

            return try {
                val refreshResponse = kotlinx.coroutines.runBlocking {
                    ApiClient.refreshService.refreshToken(mapOf("refresh" to refreshToken))
                }

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val newTokens = refreshResponse.body()!!
                    tm.saveTokens(newTokens.access, newTokens.refresh)
                    
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.access}")
                        .build()
                } else {
                    tm.clear()
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
