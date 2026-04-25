package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.ApiService
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class User(
    @SerializedName("first_name") val firstname: String,
    @SerializedName("last_name") val lastname: String,
    @SerializedName("middle_name") val patronymic: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String? = null,
    @SerializedName("role") val role: String,
    @SerializedName("id") val id: UUID? = null,
    @SerializedName("is_active") val isActive: Boolean = true
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString()?.let { UUID.fromString(it) },
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(firstname)
        parcel.writeString(lastname)
        parcel.writeString(patronymic)
        parcel.writeString(username)
        parcel.writeString(email)
        parcel.writeString(password)
        parcel.writeString(role)
        parcel.writeString(id?.toString())
        parcel.writeByte(if (isActive) 1 else 0)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User = User(parcel)
        override fun newArray(size: Int): Array<User?> = arrayOfNulls(size)
    }

    suspend fun getStudentData(): StudentData {
        return try {
            val userId = id ?: return getDefaultMockData()
            val results = ApiClient.apiService.getUserTestResults(userId)
            if (results.isNotEmpty()) {
                val avgAccuracy = results.map { it.score.toDouble() }.average()
                val avgTimeSpent = results.map { parseTime(it.completed_at, it.started_at) }.average()
                val totalAttempts = results.size
                val testCount = results.distinctBy { it.test_id }.size
                val difficultyValues = results.map { result ->
                    (result.test_id.hashCode() % 5 + 1).toDouble()
                }
                val weightedDifficulty = difficultyValues.average()

                StudentData(avgAccuracy, totalAttempts.toDouble(), avgTimeSpent, testCount.toDouble(), weightedDifficulty)
            } else {
                getDefaultMockData()
            }
        } catch (e: Exception) {
            getDefaultMockData()
        }
    }

    private fun getDefaultMockData(): StudentData {
        return StudentData(0.0, 0.0, 0.0, 0.0, 0.0)
    }

    private fun parseTime(completedAt: String?, startedAt: String?): Double {
        if (startedAt.isNullOrEmpty()) return 0.0
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val completedTime = completedAt?.let { dateFormat.parse(it)?.time } ?: System.currentTimeMillis()
            val startTime = dateFormat.parse(startedAt)?.time ?: System.currentTimeMillis()
            (completedTime - startTime) / (1000.0 * 60)
        } catch (e: Exception) { 0.0 }
    }
}
