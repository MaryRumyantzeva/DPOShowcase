package com.example.dposhowcase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseService {
    private const val TAG = "FirebaseService"
    private val db = FirebaseFirestore.getInstance()

    // Названия коллекций в Firebase
    private const val COLLECTION_ENROLLMENTS = "enrollments"
    private const val COLLECTION_COURSES = "courses"
    private const val COLLECTION_USERS = "users"

    /**
     * Сохранить заявку на курс в Firebase
     */
    suspend fun saveEnrollment(enrollment: Enrollment): Boolean {
        return try {
            val enrollmentData = hashMapOf(
                "userId" to enrollment.userId,
                "userName" to enrollment.userName,
                "userEmail" to enrollment.userEmail,
                "userPhone" to enrollment.userPhone,
                "courseId" to enrollment.courseId,
                "courseTitle" to enrollment.courseTitle,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )

            db.collection(COLLECTION_ENROLLMENTS)
                .add(enrollmentData)
                .await()

            Log.d(TAG, "Enrollment saved to Firebase successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving enrollment to Firebase", e)
            false
        }
    }

    /**
     * Получить все заявки (для админа)
     */
    suspend fun getAllEnrollments(): List<Enrollment> {
        return try {
            val result = db.collection(COLLECTION_ENROLLMENTS)
                .get()
                .await()

            result.documents.map { document ->
                Enrollment(
                    id = document.id,
                    userId = document.getString("userId") ?: "",
                    userName = document.getString("userName") ?: "",
                    userEmail = document.getString("userEmail") ?: "",
                    userPhone = document.getString("userPhone") ?: "",
                    courseId = document.getString("courseId") ?: "",
                    courseTitle = document.getString("courseTitle") ?: "",
                    timestamp = document.getLong("timestamp") ?: 0,
                    status = document.getString("status") ?: "pending"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting enrollments from Firebase", e)
            emptyList()
        }
    }

    /**
     * Получить курсы из Firebase
     */
    suspend fun getCoursesFromFirebase(): List<Course> {
        return try {
            val result = db.collection(COLLECTION_COURSES)
                .get()
                .await()

            result.documents.map { document ->
                Course(
                    id = document.id,
                    title = document.getString("title") ?: "",
                    description = document.getString("description") ?: "",
                    category = document.getString("category") ?: "",
                    duration = document.getString("duration") ?: "",
                    price = document.getDouble("price") ?: 0.0,
                    instructor = document.getString("instructor") ?: "",
                    hours = document.getLong("hours")?.toInt() ?: 0,
                    syllabus = (document.get("syllabus") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    requirements = (document.get("requirements") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    contact_email = document.getString("contact_email") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting courses from Firebase", e)
            emptyList()
        }
    }

    /**
     * Добавить тестовые курсы в Firebase (для первоначальной настройки)
     */
    suspend fun addSampleCourses() {
        val sampleCourses = listOf(
            hashMapOf(
                "title" to "Цифровой маркетинг",
                "description" to "Освойте инструменты интернет-продвижения: SEO, контекстная реклама, SMM, email-маркетинг.",
                "category" to "Маркетинг",
                "duration" to "3 месяца",
                "price" to 15000.0,
                "instructor" to "Анна Петрова",
                "hours" to 72,
                "syllabus" to listOf("Введение в цифровой маркетинг", "SEO-оптимизация", "Контекстная реклама"),
                "requirements" to listOf("Базовые знания интернета", "Умение работать с ПК"),
                "contact_email" to "marketing@dpo.ru"
            ),
            hashMapOf(
                "title" to "Анализ данных на Python",
                "description" to "Научитесь работать с большими данными, строить предсказательные модели и визуализировать результаты.",
                "category" to "IT",
                "duration" to "4 месяца",
                "price" to 20000.0,
                "instructor" to "Иван Сидоров",
                "hours" to 96,
                "syllabus" to listOf("Основы Python", "Библиотеки Pandas и NumPy", "Визуализация данных"),
                "requirements" to listOf("Базовые знания математики", "Логическое мышление"),
                "contact_email" to "data@dpo.ru"
            )
        )

        try {
            sampleCourses.forEach { courseData ->
                db.collection(COLLECTION_COURSES)
                    .add(courseData)
                    .await()
            }
            Log.d(TAG, "Sample courses added to Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding sample courses", e)
        }
    }
}