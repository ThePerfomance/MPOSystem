    package com.example.groupprojectfirsttry

    import retrofit2.Response
    import retrofit2.http.Body
    import retrofit2.http.GET
    import retrofit2.http.POST
    import retrofit2.http.Path
    import java.util.UUID

    interface ApiService {
        // Получение списка тестов
        @GET("tests")
        suspend fun getTests(): List<Test>

        @GET("tests/{testId}/questions") // Endpoint для получения вопросов
        suspend fun getQuestions(@Path("testId") testId: Int): List<Question>

        // Отправка результатов теста
        @POST("test-results")
        suspend fun submitTestResult(@Body result: TestResult): Response<SubmitResponse>

        // Вход через email и пароль
        @GET("users/by-email/{email}")
        suspend fun getUserByEmail(@Path("email") email: String): User

        // Регистрация нового пользователя
        @POST("users")
        suspend fun registerUser(@Body user: User): Response<User>
    }
    data class TestResult(
        val user_id: UUID, // Идентификатор пользователя
        val test_id: Int,  // Идентификатор теста
        val score: Int    // Оценка
    )
    data class SubmitResponse(
        val status: String,
        val message: String? = null
    )