package com.example.groupprojectfirsttry

import android.os.Parcel
import android.os.Parcelable

data class ResultItem(
    val questionText: String,
    val selectedAnswerText: String,
    val correctAnswerText: String,
    val isCorrect: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(questionText)
        parcel.writeString(selectedAnswerText)
        parcel.writeString(correctAnswerText)
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