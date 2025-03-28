package com.example.groupprojectfirsttry.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Базовый URL вашего сервера
    private const val BASE_URL = "http://10.0.2.2:3000/" // Для эмулятора Android http://10.0.2.2:3000/ 192.168.243.204

    // Инициализация Retrofit
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create()) // Конвертер для JSON
        .build()

    // Получение экземпляра API
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}