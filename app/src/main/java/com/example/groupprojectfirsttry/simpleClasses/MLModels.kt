package com.example.groupprojectfirsttry.simpleClasses

import com.google.gson.annotations.SerializedName
import java.util.UUID

// --- Базовые сущности ---

data class WeakTopic(
    val topic: String,
    @SerializedName("error_rate") val errorRate: Float,
    @SerializedName("error_count") val errorCount: Int,
    @SerializedName("total_attempts") val totalAttempts: Int,
    @SerializedName("last_error_at") val lastErrorAt: String?
)

data class RecommendationResource(
    val type: String, // "lesson", "video", "link"
    val title: String,
    val url: String,
    val content: String? = null
)

data class PersonalizedRecommendation(
    val topic: String,
    val priority: Int,
    @SerializedName("recommendation_text") val message: String,
    val resources: List<RecommendationResource>,
    @SerializedName("practice_questions") val practiceQuestions: List<Int>,
    @SerializedName("error_rate") val errorRate: Float
)

data class LearningStep(
    val step: Int,
    val action: String, // "study", "watch", "practice"
    val topic: String,
    val resource: RecommendationResource? = null,
    @SerializedName("question_ids") val questionIds: List<Int>? = null,
    @SerializedName("estimated_time_minutes") val estimatedTime: Int
)

// --- Обертки ответов API (согласно DIPLOMA_IMPLEMENTATION.md) ---

data class WeakTopicsResponse(
    @SerializedName("weak_topics") val weakTopics: List<WeakTopic>,
    @SerializedName("overall_stats") val overallStats: Map<String, Any>?
)

data class RecommendationsResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("recommendations") val recommendations: List<PersonalizedRecommendation>,
    @SerializedName("overall_stats") val overallStats: Map<String, Any>?,
    @SerializedName("progress_analysis") val progressAnalysis: ProgressAnalysis?
)

data class ProgressAnalysis(
    val trend: String, // "improving", "declining", "stable"
    val periods: List<Map<String, Any>>?,
    val recommendation: String // Текстовый совет
)

data class LearningPathResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("learning_path") val steps: List<LearningStep>,
    @SerializedName("total_estimated_time_minutes") val totalTime: Int
)
