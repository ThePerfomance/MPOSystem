package com.example.groupprojectfirsttry.api

import android.os.Parcel
import android.os.Parcelable
import com.example.groupprojectfirsttry.simpleClasses.*
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

interface ApiService {
    // Auth
    @POST("api/token/")
    suspend fun authenticateUser(@Body credentials: LoginCredentials): Response<TokenResponse>

    @POST("api/token/refresh/")
    suspend fun refreshToken(@Body refresh: Map<String, String>): Response<TokenResponse>

    // Users
    @GET("api/users/by-email/{email}/")
    suspend fun getUserByEmail(@Path("email") email: String): User

    @POST("api/users/register/")
    suspend fun registerUser(@Body registrationRequest: RegistrationRequest): Response<RegisterResponse>

    @GET("api/users/{userId}/results/")
    suspend fun getUserTestResults(@Path("userId") userId: UUID): List<TestStatistic>

    @GET("api/users/{userId}/groups/")
    suspend fun getUserGroups(@Path("userId") userId: UUID): List<Group>

    // Groups
    @GET("api/groups/")
    suspend fun getAllGroups(): List<Group>

    @GET("api/groups/{groupId}/users/")
    suspend fun getGroupUsers(@Path("groupId") groupId: UUID): List<User>

    @POST("api/group-members/")
    suspend fun addUserToGroup(@Body addUserRequest: AddUserToGroupRequest): Response<SubmitResponse>

    // Subjects, Blocks, Lessons
    @GET("api/subjects/")
    suspend fun getSubjects(): List<Subject>

    @GET("api/subjects/{subject_id}/blocks/")
    suspend fun getBlocksBySubject(@Path("subject_id") subjectId: UUID): List<Block>

    @GET("api/blocks/")
    suspend fun getAllBlocks(@Query("subject_id") subjectId: UUID? = null): List<Block>

    @GET("api/blocks/{block_id}/lessons/")
    suspend fun getLessonsByBlock(@Path("block_id") blockId: UUID): List<Lesson>

    @GET("api/blocks/")
    suspend fun getAllLessons(@Query("block_id") blockId: UUID? = null): List<Lesson>

    @GET("api/lessons/{lesson_id}/")
    suspend fun getLessonDetails(@Path("lesson_id") lessonId: UUID): Lesson

    @GET("api/lessons/{lesson_id}/test/")
    suspend fun getTestForLesson(@Path("lesson_id") lessonId: UUID): Test

    @GET("api/blocks/{block_id}/final-test/")
    suspend fun getFinalTestForBlock(@Path("block_id") blockId: UUID): Test

    // Tests
    @GET("api/tests/")
    suspend fun getTests(@Query("lesson_id") lessonId: UUID? = null, @Query("block_id") blockId: UUID? = null): List<Test>

    @GET("api/tests/{testId}/questions/")
    suspend fun getQuestions(@Path("testId") testId: Int): List<Question>

    @POST("api/test-results/")
    suspend fun submitTestResult(@Body result: TestResult): Response<TestResultResponse>

    // Training (Error Trainer)
    @GET("api/test-results/{result_id}/user-answers/")
    suspend fun getUserAnswersForResult(@Path("result_id") resultId: String): List<UserAnswer>

    @POST("api/training-sessions/from-result/{result_id}/")
    suspend fun createTrainingSession(@Path("result_id") resultId: String): Response<CreateTrainingResponse>

    @GET("api/training-sessions/")
    suspend fun getTrainingSessions(@Query("user_id") userId: UUID? = null): List<TrainingSession>

    @POST("api/training-questions/{id}/answer/")
    suspend fun submitTrainingAnswer(
        @Path("id") trainingQuestionId: Int,
        @Body answer: Map<String, Int?>
    ): Response<TrainingAnswerResponse>

    // ML
    @POST("api/ml/cluster-group/{groupId}/")
    suspend fun clusterGroup(
        @Path("groupId") groupId: UUID,
        @Body body: Map<String, Int> = emptyMap()
    ): Response<GroupClusterResponse>

    @POST("api/ml/cluster-students/")
    suspend fun clusterStudents(): Response<Any>

    // New ML Endpoints (Personalized Recommendations)
    @POST("api/ml/analyze-weak-topics/{user_id}/")
    suspend fun analyzeWeakTopics(@Path("user_id") userId: UUID): Response<WeakTopicsResponse>

    @GET("api/ml/personalized-recommendations/{user_id}/")
    suspend fun getPersonalizedRecommendations(@Path("user_id") userId: UUID): Response<RecommendationsResponse>

    @GET("api/ml/learning-path/{user_id}/")
    suspend fun getLearningPath(@Path("user_id") userId: UUID): Response<LearningPathResponse>

    // ==========================================
    // НОВЫЕ ЭНДПОИНТЫ: Группы, Предметы и Преподаватели
    // ==========================================

    // Получить предметы, доступные конкретной группе (GroupSubject)
    @GET("api/groups/{groupId}/subjects/")
    suspend fun getGroupSubjects(@Path("groupId") groupId: UUID): List<Subject>

    // Назначить предмет группе
    @POST("api/group-subjects/")
    suspend fun addSubjectToGroup(@Body request: GroupSubjectRequest): Response<SubmitResponse>

    // Получить преподавателей, закрепленных за группой (TeacherGroup)
    @GET("api/groups/{groupId}/teachers/")
    suspend fun getGroupTeachers(@Path("groupId") groupId: UUID): List<User>

    // Назначить преподавателя группе
    @POST("api/teacher-groups/")
    suspend fun addTeacherToGroup(@Body request: TeacherGroupRequest): Response<SubmitResponse>

    // Получить все группы конкретного преподавателя
    @GET("api/users/{teacherId}/teacher-groups/")
    suspend fun getTeacherGroups(@Path("teacherId") teacherId: UUID): List<Group>
}

data class RegistrationRequest(
    val email: String,
    val password: String,
    val firstname: String,
    val lastname: String,
    val patronymic: String,
    val role: String = "student"
)

data class RegisterResponse(
    val message: String,
    val user: User
)

data class CreateTrainingResponse(
    val session: TrainingSession,
    @SerializedName("added_count") val addedCount: Int,
    @SerializedName("is_new_session") val isNewSession: Boolean
)

data class TokenResponse(val access: String, val refresh: String)
data class LoginCredentials(val email: String, val password: String)

data class TestResult(
    @SerializedName("user_id") val user_id: UUID,
    @SerializedName("test_id") val test_id: Int,
    @SerializedName("earned_points") val earnedPoints: Int, // <-- Переименовано из score
    @SerializedName("total_points") val totalPoints: Int,   // <-- Новое поле
    @SerializedName("started_at") val started_at: String,
    @SerializedName("completed_at") val completed_at: String,
    @SerializedName("answers") val answers: List<TestAnswerRequest>? = null
)

data class TestResultResponse(
    val id: String?,
    @SerializedName("earned_points") val earnedPoints: Int? = null, // <-- Переименовано из score
    @SerializedName("total_points") val totalPoints: Int? = null,   // <-- Новое поле
    @SerializedName("user_id") val userId: UUID? = null,
    @SerializedName("test_id") val testId: Int? = null
)

data class TestAnswerRequest(
    @SerializedName("question_id") val question_id: Int,
    @SerializedName("chosen_answer_id") val chosen_answer_id: Int?,
    @SerializedName("is_correct") val is_correct: Boolean,
    @SerializedName("points_earned") val pointsEarned: Int = 0 // <-- Новое поле
)

data class TrainingAnswerResponse(
    @SerializedName("is_correct") val isCorrect: Boolean? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("id") val id: Int? = null
)

data class SubmitResponse(
    val status: String,
    val message: String? = null,
    val id: String? = null
)

data class TestStatistic(
    @SerializedName("user_id") val user_id: UUID,
    @SerializedName("test_id") val test_id: Int,
    @SerializedName("earned_points") val earnedPoints: Int, // <-- Переименовано из score
    @SerializedName("total_points") val totalPoints: Int,   // <-- Новое поле
    @SerializedName("started_at") val started_at: String? = null,
    @SerializedName("completed_at") var completed_at: String? = null
) : Parcelable {
    val difficulty: Int get() = (test_id % 5) + 1
    
    // Вычисляемый процент для обратной совместимости или удобства
    val score: Int get() = if (totalPoints > 0) (earnedPoints * 100) / totalPoints else 0

    constructor(parcel: Parcel) : this(
        parcel.readSerializable() as UUID,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(user_id)
        parcel.writeInt(test_id)
        parcel.writeInt(earnedPoints)
        parcel.writeInt(totalPoints)
        parcel.writeString(started_at)
        parcel.writeString(completed_at)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TestStatistic> {
        override fun createFromParcel(parcel: Parcel): TestStatistic = TestStatistic(parcel)
        override fun newArray(size: Int): Array<TestStatistic?> = arrayOfNulls(size)
    }
}

data class Group(val id: UUID, val name: String)
data class AddUserToGroupRequest(val user_id: UUID, val group_id: UUID)
data class PcaPoint(val user_id: String, val x: Float, val y: Float, val cluster_id: Int, val rank: String, val firstname: String = "", val lastname: String = "")
data class ClusterResult(val user_id: String, val rank: String, val cluster_id: Int, val avg_score: Float, val tests_taken: Int, val pca_x: Float, val pca_y: Float)
data class ClusterMetrics(val silhouette: Float, val inertia: Float)
data class GroupClusterResponse(val group_id: String, val group_name: String, val clusters: List<ClusterResult>, val pca_points: List<PcaPoint>, val metrics: ClusterMetrics)

// Запрос на привязку преподавателя к группе
data class TeacherGroupRequest(
    @SerializedName("teacher_id") val teacherId: UUID,
    @SerializedName("group_id") val groupId: UUID
)

// Запрос на привязку предмета к группе
data class GroupSubjectRequest(
    @SerializedName("group_id") val groupId: UUID,
    @SerializedName("subject_id") val subjectId: UUID
)
