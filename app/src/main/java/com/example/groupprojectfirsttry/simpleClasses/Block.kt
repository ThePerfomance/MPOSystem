package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import java.util.UUID

data class Block(
    val id: UUID,
    @SerializedName("subject") val subjectId: UUID? = null, // В Django часто поле называется 'subject' (ID объекта)
    val title: String,
    val description: String?, // Может быть null!
    @SerializedName("final_test") val finalTestId: Int?,
    @SerializedName("lessons_count") val lessonsCount: Int = 0,
    val position: Int = 0,
    @SerializedName("is_published") val isPublished: Boolean = true
) : Parcelable {
    constructor(parcel: Parcel) : this(
        UUID.fromString(parcel.readString() ?: UUID.randomUUID().toString()),
        parcel.readString()?.let { UUID.fromString(it) },
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id.toString())
        parcel.writeString(subjectId?.toString())
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeValue(finalTestId)
        parcel.writeInt(lessonsCount)
        parcel.writeInt(position)
        parcel.writeByte(if (isPublished) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Block> {
        override fun createFromParcel(parcel: Parcel): Block = Block(parcel)
        override fun newArray(size: Int): Array<Block?> = arrayOfNulls(size)
    }
}
