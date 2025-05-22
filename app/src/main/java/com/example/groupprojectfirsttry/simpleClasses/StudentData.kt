package com.example.groupprojectfirsttry.simpleClasses

data class StudentData(
    val accuracy: Double,
    val attempts: Int,
    val timeSpent: Double,
    val testCount: Int, // Количество пройденных тестов
    val weightedDifficulty: Double // Средневзвешенная сложность тестов
)
{

}
