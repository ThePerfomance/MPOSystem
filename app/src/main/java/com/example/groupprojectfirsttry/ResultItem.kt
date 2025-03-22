package com.example.groupprojectfirsttry

import android.os.Parcel
import android.os.Parcelable

data class ResultItem(
    val questionText: String,
    val answers: List<Answer>,
    val selectedAnswerText: String,
    val isCorrect: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.createTypedArrayList(Answer.CREATOR) ?: emptyList(),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(questionText)
        parcel.writeTypedList(answers)
        parcel.writeString(selectedAnswerText)
        parcel.writeByte(if (isCorrect) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ResultItem> {
        override fun createFromParcel(parcel: Parcel): ResultItem {
            return ResultItem(parcel)
        }

        override fun newArray(size: Int): Array<ResultItem?> {
            return arrayOfNulls(size)
        }
    }
}