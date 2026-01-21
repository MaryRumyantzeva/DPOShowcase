package com.example.dposhowcase

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val enrolledCourses: List<String> = emptyList()
) : Parcelable {

    // Добавляем этот метод
    fun hasEnrolledInCourse(courseId: String): Boolean {
        return enrolledCourses.contains(courseId)
    }
}