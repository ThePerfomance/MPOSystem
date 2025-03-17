package com.example.groupprojectfirsttry

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    // Получение списка тестов
    @GET("tests")
    suspend fun getTests(): List<Test>

    @GET("tests/{testId}/questions") // Endpoint для получения вопросов
    suspend fun getQuestions(@Path("testId") testId: Int): List<Question>

    @POST("test_results")
    suspend fun submitTestResult(@Body result: TestResult): Response<SubmitResponse>
}
data class TestResult(
    val testId: Int,
    val answers: List<Answer>
)
data class SubmitResponse(
    val status: String,
    val message: String? = null
)