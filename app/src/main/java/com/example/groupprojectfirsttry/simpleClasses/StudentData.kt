package com.example.groupprojectfirsttry.simpleClasses

data class StudentData(
    val accuracy: Double,
    val attempts: Double,
    val timeSpent: Double,
    val testCount: Double, // Количество пройденных тестов
    val weightedDifficulty: Double // Средневзвешенная сложность тестов
)
