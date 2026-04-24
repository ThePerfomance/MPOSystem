package com.example.groupprojectfirsttry.simpleClasses

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class WeakTopic(
    val topic: String,
    @SerializedName("error_rate") val errorRate: Float,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("wrong_answers") val wrongAnswers: Int
)

data class RecommendationResource(
    val type: String, // "video", "article", "external"
    val title: String,
    val url: String
)

data class PersonalizedRecommendation(
    val topic: String,
    val priority: String, // "High", "Medium", "Low"
    val message: String,
    val resources: List<RecommendationResource>
)

data class LearningStep(
    val step: Int,
    val action: String, // "study", "watch", "practice"
    val content: String,
    val topic: String
)

data class LearningPath(
    @SerializedName("user_id") val userId: UUID,
    @SerializedName("generated_at") val generatedAt: String,
    val steps: List<LearningStep>
)
