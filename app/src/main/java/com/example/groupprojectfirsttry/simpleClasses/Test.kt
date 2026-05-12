package com.example.groupprojectfirsttry.simpleClasses

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class TestDifficulty(
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

    companion object CREATOR : Parcelable.Creator<TestDifficulty> {
        override fun createFromParcel(parcel: Parcel): TestDifficulty = TestDifficulty(parcel)
        override fun newArray(size: Int): Array<TestDifficulty?> = arrayOfNulls(size)
    }
}

data class Test(
    val id: Int,
    val title: String,
    val description: String?,
    @SerializedName("subject_name") val subjectName: String,
    val progress: Int,
    val difficulty: TestDifficulty? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readParcelable(TestDifficulty::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(subjectName)
        parcel.writeInt(progress)
        parcel.writeParcelable(difficulty, flags)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Test> {
        override fun createFromParcel(parcel: Parcel): Test = Test(parcel)
        override fun newArray(size: Int): Array<Test?> = arrayOfNulls(size)
    }
}
