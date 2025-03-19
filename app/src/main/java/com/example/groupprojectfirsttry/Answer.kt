package com.example.groupprojectfirsttry

// Модель ответа
data class Answer(
    val id: Int,
    val questionId: Int,
    val text: String,
    val isCorrect: Boolean, // Правильность ответа (из сервера)
    var isSelected: Boolean = false // Новое поле для отслеживания выбора
)
