package com.example.groupprojectfirsttry

import retrofit2.http.GET

interface ApiService {
    // Получение списка тестов
    @GET("tests") // Замените на ваш endpoint
    suspend fun getTests(): List<Test>
}