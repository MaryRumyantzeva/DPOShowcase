package com.example.dposhowcase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object EnrollmentService {
    private const val TAG = "EnrollmentService"
    private val db = FirebaseFirestore.getInstance()

    fun saveEnrollment(course: Course, user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val enrollment = hashMapOf(
            "courseId" to course.id,
            "courseTitle" to course.title,
            "userId" to user.id,
            "userName" to user.name,
            "userEmail" to user.email,
            "userPhone" to user.phone,
            "timestamp" to System.currentTimeMillis(),
            "status" to "pending"
        )

        db.collection("enrollments")
            .add(enrollment)
            .addOnSuccessListener {
                Log.d(TAG, "Enrollment saved to Firebase")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving enrollment", e)
                onError(e.message ?: "Unknown error")
            }
    }

    fun getEnrollments(onSuccess: (List<Map<String, Any>>) -> Unit) {
        db.collection("enrollments")
            .get()
            .addOnSuccessListener { result ->
                val enrollments = result.documents.map { it.data ?: emptyMap() }
                onSuccess(enrollments)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting enrollments", e)
                onSuccess(emptyList())
            }
    }
}