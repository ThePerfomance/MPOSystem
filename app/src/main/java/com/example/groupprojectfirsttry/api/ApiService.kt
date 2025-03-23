    package com.example.groupprojectfirsttry.api

    import com.example.groupprojectfirsttry.simpleClasses.Question
    import com.example.groupprojectfirsttry.simpleClasses.Test
    import com.example.groupprojectfirsttry.simpleClasses.User
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

        // Получение результатов теста по пользователю и тесту
        @GET("users/{userId}/results")
        suspend fun getUserTestResults(@Path("userId") userId: UUID): List<TestStatistic>

        @GET("groups")
        suspend fun getAllGroups(): List<Group>

        @POST("group-members")
        suspend fun addUserToGroup(@Body addUserRequest: AddUserToGroupRequest): Response<SubmitResponse>

        @GET("users/{userId}/groups")
        suspend fun getUserGroups(@Path("userId") userId: UUID): List<Group>



    }
    data class TestResult(
        val user_id: UUID, // Идентификатор пользователя
        val test_id: Int,  // Идентификатор теста
        val score: Int,
        val completed_at: String? = null// Оценка
    )
    data class SubmitResponse(
        val status: String,
        val message: String? = null
    )
    data class TestStatistic(
        val user_id: UUID,
        val test_id: Int,
        val score: Int,
        var completed_at: String
    )
    data class Group(
        val id:UUID,
        val name:String
    )
    data class AddUserToGroupRequest(
        val user_id: UUID,
        val group_id: UUID
    )
