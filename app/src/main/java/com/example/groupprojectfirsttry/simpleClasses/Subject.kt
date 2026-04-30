package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import java.util.UUID

data class Subject(
    val id: UUID,
    val name: String,
    @SerializedName("creator_id") val creatorId: UUID? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        UUID.fromString(parcel.readString() ?: ""),
        parcel.readString() ?: "",
        parcel.readString()?.let { UUID.fromString(it) }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id.toString())
        parcel.writeString(name)
        parcel.writeString(creatorId?.toString())
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Subject> {
        override fun createFromParcel(parcel: Parcel): Subject = Subject(parcel)
        override fun newArray(size: Int): Array<Subject?> = arrayOfNulls(size)
    }
}
