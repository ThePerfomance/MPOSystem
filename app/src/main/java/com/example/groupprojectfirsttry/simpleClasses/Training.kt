package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import java.util.UUID

data class UserAnswer(
    val id: Int,
    @SerializedName("test_result_id") val testResultId: Int,
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("chosen_answer_id") val chosenAnswerId: Int,
    @SerializedName("is_correct") val isCorrect: Boolean,
    @SerializedName("answered_at") val answeredAt: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(testResultId)
        parcel.writeInt(questionId)
        parcel.writeInt(chosenAnswerId)
        parcel.writeByte(if (isCorrect) 1 else 0)
        parcel.writeString(answeredAt)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<UserAnswer> {
        override fun createFromParcel(parcel: Parcel): UserAnswer = UserAnswer(parcel)
        override fun newArray(size: Int): Array<UserAnswer?> = arrayOfNulls(size)
    }
}

data class TrainingSession(
    val id: Int,
    @SerializedName("user_id") val userId: UUID,
    @SerializedName("original_result_id") val originalResultId: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_completed") val isCompleted: Boolean,
    val questions: List<TrainingQuestion>? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        UUID.fromString(parcel.readString() ?: ""),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        mutableListOf<TrainingQuestion>().apply {
            parcel.readList(this, TrainingQuestion::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(userId.toString())
        parcel.writeInt(originalResultId)
        parcel.writeString(createdAt)
        parcel.writeByte(if (isCompleted) 1 else 0)
        parcel.writeList(questions)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TrainingSession> {
        override fun createFromParcel(parcel: Parcel): TrainingSession = TrainingSession(parcel)
        override fun newArray(size: Int): Array<TrainingSession?> = arrayOfNulls(size)
    }
}

data class TrainingQuestion(
    val id: Int,
    @SerializedName("session_id") val sessionId: Int,
    val question: Question,
    @SerializedName("is_resolved") val isResolved: Boolean,
    @SerializedName("attempts_count") val attemptsCount: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readParcelable(Question::class.java.classLoader)!!,
        parcel.readByte() != 0.toByte(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(sessionId)
        parcel.writeParcelable(question, flags)
        parcel.writeByte(if (isResolved) 1 else 0)
        parcel.writeInt(attemptsCount)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TrainingQuestion> {
        override fun createFromParcel(parcel: Parcel): TrainingQuestion = TrainingQuestion(parcel)
        override fun newArray(size: Int): Array<TrainingQuestion?> = arrayOfNulls(size)
    }
}
