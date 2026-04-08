package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import java.util.UUID

data class Lesson(
    val id: UUID,
    val block: UUID,
    val test: Int?,
    val summary: String?,
    @SerializedName("video_link") val videoLink: String?,
    @SerializedName("video_duration") val videoDuration: Int,
    val duration: Int,
    val position: Int,
    @SerializedName("is_published") val isPublished: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        UUID.fromString(parcel.readString() ?: ""),
        UUID.fromString(parcel.readString() ?: ""),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id.toString())
        parcel.writeString(block.toString())
        parcel.writeValue(test)
        parcel.writeString(summary)
        parcel.writeString(videoLink)
        parcel.writeInt(videoDuration)
        parcel.writeInt(duration)
        parcel.writeInt(position)
        parcel.writeByte(if (isPublished) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Lesson> {
        override fun createFromParcel(parcel: Parcel): Lesson = Lesson(parcel)
        override fun newArray(size: Int): Array<Lesson?> = arrayOfNulls(size)
    }
}
