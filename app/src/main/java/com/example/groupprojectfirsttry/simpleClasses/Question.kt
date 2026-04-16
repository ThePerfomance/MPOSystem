package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

// Модель вопроса
data class Question(
    val id: Int,
    val test_id: Int,
    val text: String,
    val answers: List<Answer>,
    @SerializedName("recommendation_link") val recommendationLink: String? = null,
    @SerializedName("recommendation_video_link") val recommendationVideoLink: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        mutableListOf<Answer>().apply {
            parcel.readList(this, Answer::class.java.classLoader)
        },
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(test_id)
        parcel.writeString(text)
        parcel.writeList(answers)
        parcel.writeString(recommendationLink)
        parcel.writeString(recommendationVideoLink)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Question> {
        override fun createFromParcel(parcel: Parcel): Question = Question(parcel)
        override fun newArray(size: Int): Array<Question?> = arrayOfNulls(size)
    }
}
