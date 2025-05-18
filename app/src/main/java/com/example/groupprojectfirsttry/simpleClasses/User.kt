package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.ApiService
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

data class User(
    @SerializedName("firstname") val firstname: String,
    @SerializedName("lastname") val lastname: String,
    @SerializedName("patronymic") val patronymic: String,
    @SerializedName("email") val email: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("role") val role: String,
    @SerializedName("id") val id: UUID? = null // Добавьте nullable поле id
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        UUID.fromString(parcel.readString() ?: "") // Чтение UUID из строки
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(firstname)
        parcel.writeString(lastname)
        parcel.writeString(patronymic)
        parcel.writeString(email)
        parcel.writeString(passwordHash)
        parcel.writeString(role)
        parcel.writeString(id?.toString()) // Запись UUID в виде строки
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User = User(parcel)
        override fun newArray(size: Int): Array<User?> = arrayOfNulls(size)
    }
    suspend fun getStudentData(): StudentData {
        return try {
            val results = ApiClient.apiService.getUserTestResults(id!!)
            if (results.isNotEmpty()) {
                // Вычисляем среднюю точность
                val avgAccuracy = results.map { it.score.toDouble() }.average().toDouble()

                // Вычисляем среднее время выполнения
                val avgTimeSpent = results.map { parseTime(it.completed_at, it.started_at) }.average().toDouble()

                // Количество попыток
                val totalAttempts = results.size

                // Количество уникальных тестов
                val testCount = results.distinctBy { it.test_id }.size

                // Вычисляем средневзвешенную сложность тестов
                val totalWeight = results.sumOf { it.difficulty.toDouble() }
                val weightedDifficulty = if (testCount > 0) (totalWeight / testCount).toDouble() else 1.0

                // Создаем объект StudentData
                StudentData(avgAccuracy, totalAttempts, avgTimeSpent, testCount, weightedDifficulty)
            } else {
                getDefaultMockData()
            }
        } catch (e: Exception) {
            getDefaultMockData()
        }
    }

    private fun getDefaultMockData(): StudentData {
        return StudentData(72.0, 2, 45.0, 1, 3.0) // Заглушка
    }

    private fun parseTime(completedAt: String?, startedAt: String): Double {
        return try {
            // Формат даты (например, "2023-10-05T14:48:00")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

            // Парсим время завершения (если оно есть)
            val completedTime = completedAt?.let { dateFormat.parse(it)?.time } ?: System.currentTimeMillis()

            // Парсим время начала
            val startTime = dateFormat.parse(startedAt)?.time ?: System.currentTimeMillis()

            // Разница во времени в минутах
            (completedTime - startTime) / (1000 * 60).toDouble()
        } catch (e: Exception) {
            // В случае ошибки парсинга возвращаем заглушку
            45.0
        }
    }
}