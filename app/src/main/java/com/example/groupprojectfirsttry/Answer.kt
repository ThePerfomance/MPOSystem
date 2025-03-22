package com.example.groupprojectfirsttry

// Модель ответа
data class Answer(
    val id: Int,
    val question_id: Int,
    val text: String,
    val is_correct: Boolean
)