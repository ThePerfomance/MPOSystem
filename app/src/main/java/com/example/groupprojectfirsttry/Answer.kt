package com.example.groupprojectfirsttry

// Модель ответа
data class Answer(
    val id: Int,
    val text: String,
    var isSelected: Boolean = false
)
