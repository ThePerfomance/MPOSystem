package com.example.groupprojectfirsttry.simpleClasses

// Модель вопроса
data class Question(
    val id: Int,
    val test_id: Int,
    val text: String,
    val answers: List<Answer>
)