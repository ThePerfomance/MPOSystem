package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable

data class Test(
    val id: Int,
    val title: String,
    val description: String?,
    val subjectName: String,
    val progress: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(subjectName)
        parcel.writeInt(progress)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Test> {
        override fun createFromParcel(parcel: Parcel): Test = Test(parcel)
        override fun newArray(size: Int): Array<Test?> = arrayOfNulls(size)
    }
}
