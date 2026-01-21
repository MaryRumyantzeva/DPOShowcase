package com.example.dposhowcase

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sharedPrefManager = SharedPrefManager(this)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sharedPrefManager.clearUser()
            Toast.makeText(this, "Вы вышли из профиля", Toast.LENGTH_SHORT).show()
            updateUserInfo()
        }

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            showRegistrationDialog()
        }

        updateUserInfo()
    }

    private fun updateUserInfo() {
        val user = sharedPrefManager.getUser()

        if (user == null) {
            findViewById<TextView>(R.id.tvUserName).text = "Гость"
            findViewById<TextView>(R.id.tvUserEmail).text = "Войдите в систему"
            findViewById<TextView>(R.id.tvUserPhone).text = ""
            findViewById<TextView>(R.id.tvEnrolledCount).text = ""
            findViewById<TextView>(R.id.tvEnrolledCourses).text = ""
            findViewById<Button>(R.id.btnLogin).visibility = android.view.View.VISIBLE
            findViewById<Button>(R.id.btnLogout).visibility = android.view.View.GONE
        } else {
            findViewById<TextView>(R.id.tvUserName).text = user.name
            findViewById<TextView>(R.id.tvUserEmail).text = "Email: ${user.email}"
            findViewById<TextView>(R.id.tvUserPhone).text = "Телефон: ${if (user.phone.isBlank()) "не указан" else user.phone}"
            findViewById<TextView>(R.id.tvEnrolledCount).text =
                "Записан на курсов: ${user.enrolledCourses.size}"

            findViewById<Button>(R.id.btnLogin).visibility = android.view.View.GONE
            findViewById<Button>(R.id.btnLogout).visibility = android.view.View.VISIBLE

            // Простое сообщение о курсах
            findViewById<TextView>(R.id.tvEnrolledCourses).text =
                if (user.enrolledCourses.isNotEmpty()) "Есть записанные курсы"
                else "Нет записанных курсов"
        }
    }

    private fun showRegistrationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)

        MaterialAlertDialogBuilder(this)
            .setTitle("Регистрация / Вход")
            .setMessage("Введите email. Если вы уже регистрировались, ваши данные восстановятся.")
            .setView(dialogView)
            .setPositiveButton("Продолжить") { _, _ ->
                val name = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)?.text?.toString()?.trim() ?: ""
                val email = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)?.text?.toString()?.trim()?.lowercase() ?: ""
                val phone = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPhone)?.text?.toString()?.trim() ?: ""

                if (name.isBlank() || email.isBlank()) {
                    Toast.makeText(this, "Заполните имя и email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Шаг 1: Пытаемся найти существующего пользователя
                val existingUserId = sharedPrefManager.findUserIdByEmail(email)
                    ?: if (phone.isNotBlank()) sharedPrefManager.findUserIdByPhone(phone) else null

                if (existingUserId != null) {
                    // Нашли существующего пользователя
                    val existingUser = sharedPrefManager.getUserById(existingUserId)
                    if (existingUser != null) {
                        // Восстанавливаем данные
                        sharedPrefManager.saveUser(existingUser)
                        Toast.makeText(
                            this,
                            " Добро пожаловать, ${existingUser.name}!\nВаши данные восстановлены.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Новый пользователь
                    val userId = "user_${System.currentTimeMillis()}_${email.hashCode()}"
                    val newUser = User(
                        id = userId,
                        name = name,
                        email = email,
                        phone = phone,
                        enrolledCourses = emptyList()
                    )

                    // Сохраняем
                    sharedPrefManager.addOrUpdateUser(newUser)
                    Toast.makeText(
                        this,
                        " Регистрация успешна!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Обновляем и закрываем
                updateUserInfo()
                finish()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun findExistingUser(email: String, phone: String): User? {
        // Пока просто проверяем локально
        val currentUser = sharedPrefManager.getUser()
        return if (currentUser != null &&
            (currentUser.email.equals(email, ignoreCase = true) ||
                    (phone.isNotBlank() && currentUser.phone == phone))) {
            currentUser
        } else {
            null
        }
    }

    private fun generateUserId(email: String, phone: String): String {
        val timestamp = System.currentTimeMillis()
        val phoneHash = if (phone.isNotBlank()) phone.hashCode() else 0
        return "${timestamp}_${email.hashCode()}_${phoneHash}"
    }

    private fun saveToFirebaseInBackground(user: User) {
        // Сохраняем в Firebase в фоне, но не блокируем пользователя
        Thread {
            try {
                // Просто пытаемся сохранить, но не критично если не получится
                kotlin.runCatching {
                    kotlinx.coroutines.runBlocking {
                        FirebaseRepository.saveUserToFirestore(user)
                    }
                }
            } catch (e: Exception) {
                // Игнорируем ошибки Firebase для регистрации
                android.util.Log.e("ProfileActivity", "Firebase error", e)
            }
        }.start()
    }
}