package com.example.dposhowcase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseRepository {
    private const val TAG = "FirebaseRepository"
    private val db = FirebaseFirestore.getInstance()

    // Названия коллекций
    private const val COLLECTION_COURSES = "courses"
    private const val COLLECTION_ENROLLMENTS = "enrollments"

    // Получить все курсы из Firestore
    suspend fun getCoursesFromFirestore(): List<Course> {
        return try {
            Log.d(TAG, "Загрузка курсов из Firestore...")

            val result = db.collection(COLLECTION_COURSES)
                .get()
                .await()

            val courses = result.documents.map { document ->
                Course(
                    id = document.id,  // Используем Firestore document ID
                    title = document.getString("title") ?: "",
                    description = document.getString("description") ?: "",
                    category = document.getString("category") ?: "",
                    duration = document.getString("duration") ?: "",
                    price = document.getDouble("price") ?: 0.0,
                    instructor = document.getString("instructor") ?: "",
                    hours = document.getLong("hours")?.toInt() ?: 0,
                    syllabus = convertToList(document.get("syllabus")),
                    requirements = convertToList(document.get("requirements")),
                    contact_email = document.getString("contact_email") ?: ""
                )
            }

            Log.d(TAG, "Загружено ${courses.size} курсов из Firestore")
            courses
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки курсов из Firestore", e)
            emptyList()
        }
    }

    // Вспомогательная функция для конвертации в List<String>
    private fun convertToList(data: Any?): List<String> {
        return when (data) {
            is List<*> -> data.filterIsInstance<String>()
            else -> emptyList()
        }
    }

    // Сохранить заявку на курс
    suspend fun saveEnrollmentToFirestore(
        course: Course,
        user: User
    ): Boolean {
        return try {
            Log.d(TAG, "Сохранение заявки в Firestore: ${course.title} - ${user.name}")

            val enrollmentData = hashMapOf(
                "userId" to user.id,
                "userName" to user.name,
                "userEmail" to user.email,
                "userPhone" to user.phone,
                "courseId" to course.id,
                "courseTitle" to course.title,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending",
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            db.collection(COLLECTION_ENROLLMENTS)
                .add(enrollmentData)
                .await()

            Log.d(TAG, "Заявка успешно сохранена в Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения заявки в Firestore", e)
            false
        }
    }

    // Получить все заявки (для админа)
    suspend fun getAllEnrollmentsFromFirestore(): List<Map<String, Any>> {
        return try {
            Log.d(TAG, "Загрузка заявок из Firestore...")

            val result = db.collection(COLLECTION_ENROLLMENTS)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val enrollments = result.documents.map { document ->
                val data = document.data ?: emptyMap()
                mutableMapOf<String, Any>().apply {
                    putAll(data)
                    put("id", document.id)
                }
            }

            Log.d(TAG, "Загружено ${enrollments.size} заявок из Firestore")
            enrollments
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки заявок из Firestore", e)
            emptyList()
        }
    }

    // Добавить тестовые курсы (если база пустая)
    suspend fun addSampleCoursesIfNeeded() {
        try {
            val existingCourses = db.collection(COLLECTION_COURSES)
                .limit(1)
                .get()
                .await()

            if (existingCourses.isEmpty) {
                Log.d(TAG, "База курсов пустая, добавляем тестовые данные...")
                addSampleCourses()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки наличия курсов", e)
        }
    }

    private suspend fun addSampleCourses() {
        val sampleCourses = listOf(
            hashMapOf(
                "title" to "Цифровой маркетинг",
                "description" to "Освойте инструменты интернет-продвижения",
                "category" to "Маркетинг",
                "duration" to "3 месяца",
                "price" to 15000.0,
                "instructor" to "Анна Петрова",
                "hours" to 72,
                "syllabus" to listOf("SEO", "Контекстная реклама", "SMM"),
                "requirements" to listOf("Базовые знания интернета"),
                "contact_email" to "marketing@dpo.ru"
            ),
            hashMapOf(
                "title" to "Анализ данных на Python",
                "description" to "Научитесь работать с большими данными",
                "category" to "IT",
                "duration" to "4 месяца",
                "price" to 20000.0,
                "instructor" to "Иван Сидоров",
                "hours" to 96,
                "syllabus" to listOf("Python", "Pandas", "NumPy"),
                "requirements" to listOf("Базовые знания математики"),
                "contact_email" to "data@dpo.ru"
            ),
            hashMapOf(
                "title" to "Управление проектами",
                "description" to "Освойте методики Agile и Scrum",
                "category" to "Менеджмент",
                "duration" to "2 месяца",
                "price" to 12000.0,
                "instructor" to "Мария Иванова",
                "hours" to 48,
                "syllabus" to listOf("Agile", "Scrum", "Управление рисками"),
                "requirements" to listOf("Опыт работы в команде"),
                "contact_email" to "pm@dpo.ru"
            )
        )

        try {
            for (courseData in sampleCourses) {
                db.collection(COLLECTION_COURSES)
                    .add(courseData)
                    .await()
            }
            Log.d(TAG, "Тестовые курсы добавлены в Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка добавления тестовых курсов", e)
        }
    }

    private const val COLLECTION_USERS = "users"

    // Сохранить пользователя в Firebase
    suspend fun saveUserToFirestore(user: User): Boolean {
        return try {
            Log.d(TAG, "Сохранение пользователя в Firestore: ${user.email}")

            val userData = hashMapOf(
                "id" to user.id,
                "name" to user.name,
                "email" to user.email,
                "phone" to user.phone,
                "enrolledCourses" to user.enrolledCourses,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )

            db.collection(COLLECTION_USERS)
                .document(user.id) // Используем userId как ID документа
                .set(userData)
                .await()

            Log.d(TAG, "Пользователь сохранен в Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения пользователя в Firestore", e)
            false
        }
    }

    // Найти пользователя по email
    suspend fun findUserByEmail(email: String): User? {
        return try {
            Log.d(TAG, "Поиск пользователя по email: $email")

            val result = db.collection(COLLECTION_USERS)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (result.documents.isNotEmpty()) {
                val document = result.documents[0]
                val user = User(
                    id = document.getString("id") ?: "",
                    name = document.getString("name") ?: "",
                    email = document.getString("email") ?: "",
                    phone = document.getString("phone") ?: "",
                    enrolledCourses = (document.get("enrolledCourses") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
                Log.d(TAG, "Пользователь найден: ${user.name}")
                user
            } else {
                Log.d(TAG, "Пользователь не найден")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка поиска пользователя", e)
            null
        }
    }

    // Обновить список курсов пользователя
    suspend fun updateUserCourses(userId: String, courseIds: List<String>): Boolean {
        return try {
            Log.d(TAG, "Обновление курсов пользователя $userId")

            val updateData = hashMapOf<String, Any>(
                "enrolledCourses" to courseIds,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )

            db.collection(COLLECTION_USERS)
                .document(userId)
                .update(updateData)
                .await()

            Log.d(TAG, "Курсы пользователя обновлены")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления курсов пользователя", e)
            false
        }
    }

    // Методы для администратора
    suspend fun getAllEnrollmentsForAdmin(): List<Enrollment> {
        return try {
            Log.d(TAG, "Загрузка всех заявок для администратора")

            val result = db.collection(COLLECTION_ENROLLMENTS)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
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
            Log.e(TAG, "Ошибка загрузки заявок", e)
            emptyList()
        }
    }

    suspend fun updateEnrollmentStatus(enrollmentId: String, status: String): Boolean {
        return try {
            Log.d(TAG, "Обновление статуса заявки $enrollmentId на $status")

            val updateData = hashMapOf<String, Any>(
                "status" to status,
                "processedAt" to com.google.firebase.Timestamp.now()
            )

            db.collection(COLLECTION_ENROLLMENTS)
                .document(enrollmentId)
                .update(updateData)
                .await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления статуса", e)
            false
        }
    }

    // Проверка является ли пользователь админом
    suspend fun isAdmin(email: String): Boolean {
        return try {
            // Простая проверка - если email содержит "admin" или определенный домен
            email.contains("admin") || email.endsWith("@dpo.ru")
        } catch (e: Exception) {
            false
        }
    }
}