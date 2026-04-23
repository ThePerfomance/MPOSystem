package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import java.util.UUID

data class Lesson(
    val id: UUID,
    val block: UUID,
    val title: String,
    val test: Int?,
    // Пробуем считать и description, и summary для совместимости
    @SerializedName(value = "description", alternate = ["summary", "content"]) 
    val summary: String?,
    val video: Video? = null,
    val duration: Int,
    val position: Int,
    @SerializedName("is_published") val isPublished: Boolean = true
) : Parcelable {
    constructor(parcel: Parcel) : this(
        UUID.fromString(parcel.readString() ?: ""),
        UUID.fromString(parcel.readString() ?: ""),
        parcel.readString() ?: "",
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readParcelable(Video::class.java.classLoader),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id.toString())
        parcel.writeString(block.toString())
        parcel.writeString(title)
        parcel.writeValue(test)
        parcel.writeString(summary)
        parcel.writeParcelable(video, flags)
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

data class Video(
    val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val duration: Int,
    @SerializedName("final_link") val finalLink: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(type)
        parcel.writeInt(duration)
        parcel.writeString(finalLink)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Video> {
        override fun createFromParcel(parcel: Parcel): Video = Video(parcel)
        override fun newArray(size: Int): Array<Video?> = arrayOfNulls(size)
    }
}
