package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import java.util.UUID

data class UserAnswer(
    val id: UUID,
    @SerializedName("test_result") val testResultId: UUID,
    val question: Int,
    @SerializedName("chosen_answers") val chosenAnswers: List<Int> = emptyList(),
    @SerializedName("is_correct") val isCorrect: Boolean,
    @SerializedName("answered_at") val answeredAt: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        UUID.fromString(parcel.readString() ?: ""),
        UUID.fromString(parcel.readString() ?: ""),
        parcel.readInt(),
        mutableListOf<Int>().apply { parcel.readList(this, Int::class.java.classLoader) },
        parcel.readByte() != 0.toByte(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id.toString())
        parcel.writeString(testResultId.toString())
        parcel.writeInt(question)
        parcel.writeList(chosenAnswers)
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
    val id: UUID,
    @SerializedName("user") val userId: UUID,
    @SerializedName("lesson") val lessonId: UUID? = null,
    @SerializedName("lesson_title") val lessonTitle: String? = null,
    @SerializedName("source_test_result") val sourceTestResultId: UUID? = null,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("training_questions") val questions: List<TrainingQuestion>? = null
) : Parcelable {
    val isCompleted: Boolean
        get() = status == "completed"

    constructor(parcel: Parcel) : this(
        UUID.fromString(parcel.readString() ?: ""),
        UUID.fromString(parcel.readString() ?: ""),
        parcel.readString()?.let { UUID.fromString(it) },
        parcel.readString(),
        parcel.readString()?.let { UUID.fromString(it) },
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.createTypedArrayList(TrainingQuestion.CREATOR)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id.toString())
        parcel.writeString(userId.toString())
        parcel.writeString(lessonId?.toString())
        parcel.writeString(lessonTitle)
        parcel.writeString(sourceTestResultId?.toString())
        parcel.writeString(status)
        parcel.writeString(createdAt)
        parcel.writeString(completedAt)
        parcel.writeTypedList(questions)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TrainingSession> {
        override fun createFromParcel(parcel: Parcel): TrainingSession = TrainingSession(parcel)
        override fun newArray(size: Int): Array<TrainingSession?> = arrayOfNulls(size)
    }
}

data class TrainingQuestion(
    val id: Int,
    @SerializedName("session") val sessionId: UUID,
    @SerializedName("question_details") val question: Question? = null,
    val status: String,
    @SerializedName("attempts_count") val attemptsCount: Int = 0,
    val position: Int = 0
) : Parcelable {
    val isResolved: Boolean
        get() = status == "correct"
    
    val hasDetails: Boolean 
        get() = question != null

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        UUID.fromString(parcel.readString() ?: ""),
        parcel.readParcelable(Question::class.java.classLoader),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(sessionId.toString())
        parcel.writeParcelable(question, flags)
        parcel.writeString(status)
        parcel.writeInt(attemptsCount)
        parcel.writeInt(position)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TrainingQuestion> {
        override fun createFromParcel(parcel: Parcel): TrainingQuestion = TrainingQuestion(parcel)
        override fun newArray(size: Int): Array<TrainingQuestion?> = arrayOfNulls(size)
    }
}
