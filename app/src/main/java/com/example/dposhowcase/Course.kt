package com.example.dposhowcase

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Course(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val duration: String = "",
    val price: Double = 0.0,
    val instructor: String = "",
    val hours: Int = 0,
    val syllabus: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val contact_email: String = ""
) : Parcelable {
    fun getFormattedPrice(): String = "%.0f ₽".format(price)
}