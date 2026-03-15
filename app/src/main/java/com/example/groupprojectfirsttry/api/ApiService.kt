    package com.example.groupprojectfirsttry.api

    import android.os.Parcel
    import android.os.Parcelable
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

        @GET("groups/{groupId}/users")
        suspend fun getGroupUsers(@Path("groupId") groupId: UUID): List<User>

        @POST("ml/cluster-group/{groupId}")
        suspend fun clusterGroup(
            @Path("groupId") groupId: UUID,
            @Body body: Map<String, Int> = emptyMap()
        ): Response<GroupClusterResponse>

        @POST("ml/cluster-students")
        suspend fun clusterStudents(): Response<Any>

    }
    data class TestResult(
        val user_id: UUID,
        val test_id: Int,
        val score: Int,
        val started_at:String,
        val completed_at: String
    )
    data class SubmitResponse(
        val status: String,
        val message: String? = null
    )
    data class TestStatistic(
        val user_id: UUID,
        val test_id: Int,
        val score: Int,
        val started_at: String? = null,
        var completed_at: String? = null

    ) : Parcelable {
        //
        val difficulty: Int
            get() = (test_id % 5) + 1 // Сложность в диапазоне [1, 5]
        //
        constructor(parcel: Parcel) : this(
            parcel.readSerializable() as UUID,
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString() ?: "",
            parcel.readString()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeSerializable(user_id)
            parcel.writeInt(test_id)
            parcel.writeInt(score)
            parcel.writeString(started_at)
            parcel.writeString(completed_at)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<TestStatistic> {
            override fun createFromParcel(parcel: Parcel): TestStatistic {
                return TestStatistic(parcel)
            }

            override fun newArray(size: Int): Array<TestStatistic?> {
                return arrayOfNulls(size)
            }
        }
    }
        data class Group(
            val id:UUID,
            val name:String
        )
    data class AddUserToGroupRequest(
        val user_id: UUID,
        val group_id: UUID
    )
    data class PcaPoint(
        val user_id: String,
        val x: Float,
        val y: Float,
        val cluster_id: Int,
        val rank: String,
        val firstname: String = "",
        val lastname: String = ""
    )

    data class ClusterResult(
        val user_id: String,
        val rank: String,
        val cluster_id: Int,
        val avg_score: Float,
        val tests_taken: Int,
        val pca_x: Float,
        val pca_y: Float
    )

    data class ClusterMetrics(
        val silhouette: Float,
        val inertia: Float
    )

    data class GroupClusterResponse(
        val group_id: String,
        val group_name: String,
        val clusters: List<ClusterResult>,
        val pca_points: List<PcaPoint>,
        val metrics: ClusterMetrics
    )

