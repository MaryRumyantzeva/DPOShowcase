package com.example.dposhowcase

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class SharedPrefManager(context: Context) {
    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("dpo_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUser(user: User) {
        val json = gson.toJson(user)
        sharedPref.edit().apply {
            putString("current_user", json)

            // Сохраняем связь email -> userId для поиска
            if (user.email.isNotBlank()) {
                putString("email_${user.email.lowercase()}", user.id)
            }

            // Сохраняем связь телефон -> userId для поиска
            if (user.phone.isNotBlank()) {
                putString("phone_${user.phone}", user.id)
            }
        }.apply()
    }

    fun getUser(): User? {
        val json = sharedPref.getString("current_user", null)
        return if (json != null) gson.fromJson(json, User::class.java) else null
    }

    // Найти ID пользователя по email
    fun findUserIdByEmail(email: String): String? {
        return sharedPref.getString("email_${email.lowercase()}", null)
    }

    // Найти ID пользователя по телефону
    fun findUserIdByPhone(phone: String): String? {
        return sharedPref.getString("phone_$phone", null)
    }

    // Загрузить пользователя по ID
    fun getUserById(userId: String): User? {
        // Ищем во всех сохраненных пользователях
        // Для простоты храним всех пользователей в одном JSON
        val allUsersJson = sharedPref.getString("all_users", "{}")
        val allUsers = gson.fromJson(allUsersJson, Map::class.java) as? Map<String, String> ?: emptyMap()

        val userJson = allUsers[userId]
        return if (userJson != null) gson.fromJson(userJson, User::class.java) else null
    }

    // Сохранить всех пользователей
    private fun saveAllUsers(users: Map<String, User>) {
        val usersMap = users.mapValues { gson.toJson(it.value) }
        val json = gson.toJson(usersMap)
        sharedPref.edit().putString("all_users", json).apply()
    }

    fun addOrUpdateUser(user: User) {
        // Загружаем всех пользователей
        val allUsersJson = sharedPref.getString("all_users", "{}")
        val allUsers = gson.fromJson(allUsersJson, Map::class.java) as? MutableMap<String, String> ?: mutableMapOf()

        // Добавляем/обновляем пользователя
        allUsers[user.id] = gson.toJson(user)

        // Сохраняем обратно
        val json = gson.toJson(allUsers)
        sharedPref.edit().putString("all_users", json).apply()

        // Также сохраняем как текущего
        saveUser(user)
    }

    fun isUserLoggedIn(): Boolean = getUser() != null

    fun clearUser() {
        sharedPref.edit().remove("current_user").apply()
    }

    fun addEnrolledCourse(courseId: String) {
        val user = getUser() ?: return
        val updatedCourses = user.enrolledCourses + courseId
        val updatedUser = user.copy(enrolledCourses = updatedCourses)
        saveUser(updatedUser)
        addOrUpdateUser(updatedUser) // Сохраняем в общий список
    }

    fun getAllUsers(): List<User> {
        val users = mutableListOf<User>()

        // Загружаем всех пользователей из "all_users"
        val allUsersJson = sharedPref.getString("all_users", "{}")
        val allUsersMap = gson.fromJson(allUsersJson, Map::class.java) as? Map<String, String> ?: emptyMap()

        for ((userId, userJson) in allUsersMap) {
            try {
                val user = gson.fromJson(userJson, User::class.java)
                users.add(user)
            } catch (e: Exception) {
                // Логирование ошибки, если нужно
                e.printStackTrace()
            }
        }

        return users
    }
}