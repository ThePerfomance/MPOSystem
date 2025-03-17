package com.example.groupprojectfirsttry

data class Test(
    val id: Int,
    val title: String,          // Название теста (например, "Глава 1: Основы программирования")
    val description: String?,   // Описание теста
    val subjectName: String,    // Название предмета (из таблицы subjects)
    val progress: Int           // Прогресс студента (например, 75%)
)
