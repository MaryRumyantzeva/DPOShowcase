package com.example.dposhowcase

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Enrollment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val timestamp: Long = 0,
    val status: String = "pending" // pending, approved, rejected
) : Parcelable {

    fun getFormattedDate(): String {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        return formatter.format(date)
    }
}