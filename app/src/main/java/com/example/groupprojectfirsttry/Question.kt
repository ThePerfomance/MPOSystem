package com.example.groupprojectfirsttry

// Модель вопроса
data class Question(
    val id: Int,
    val text: String,
    val answers: List<Answer>
)
