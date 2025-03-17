package com.example.groupprojectfirsttry

import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    // Получение списка тестов
    @GET("tests") // Замените на ваш endpoint
    suspend fun getTests(): List<Test>

    @GET("tests/{testId}/questions") // Endpoint для получения вопросов
    suspend fun getQuestions(@Path("testId") testId: Int): List<Question>
}