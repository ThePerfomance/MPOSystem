package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

// Модель сложности
data class QuestionDifficulty(
    @SerializedName("difficulty") val level: String, // "easy", "medium", "hard"
    @SerializedName("avg_score") val avgScore: Double
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "medium",
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(level)
        parcel.writeDouble(avgScore)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<QuestionDifficulty> {
        override fun createFromParcel(parcel: Parcel): QuestionDifficulty = QuestionDifficulty(parcel)
        override fun newArray(size: Int): Array<QuestionDifficulty?> = arrayOfNulls(size)
    }
}

// Модель вопроса
data class Question(
    val id: Int,
    @SerializedName("test") val testId: Int,
    val text: String,
    val answers: List<Answer>,
    @SerializedName("recommendation_link") val recommendationLink: String? = null,
    @SerializedName("recommendation_video_link") val recommendationVideoLink: String? = null,
    @SerializedName("difficulty") val difficulty: QuestionDifficulty? = null,
    @SerializedName("explanation") val explanation: String? = null,
    @SerializedName("is_multiple_choice") val isMultipleChoice: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.createTypedArrayList(Answer.CREATOR) ?: emptyList(),
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(QuestionDifficulty::class.java.classLoader),
        parcel.readString(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(testId)
        parcel.writeString(text)
        parcel.writeTypedList(answers)
        parcel.writeString(recommendationLink)
        parcel.writeString(recommendationVideoLink)
        parcel.writeParcelable(difficulty, flags)
        parcel.writeString(explanation)
        parcel.writeByte(if (isMultipleChoice) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Question> {
        override fun createFromParcel(parcel: Parcel): Question = Question(parcel)
        override fun newArray(size: Int): Array<Question?> = arrayOfNulls(size)
    }
}
