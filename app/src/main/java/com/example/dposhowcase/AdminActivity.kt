package com.example.dposhowcase

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking

class AdminActivity : AppCompatActivity() {

    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPrefManager = SharedPrefManager(this)

        // Создаем простой интерфейс
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val title = TextView(this).apply {
            text = "⚙️ Панель администратора"
            textSize = 20f
            setPadding(0, 0, 0, 30)
        }

        // Информация о текущем админе
        val tvAdminInfo = TextView(this).apply {
            text = getAdminInfo()
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }

        val btnViewUsers = Button(this).apply {
            text = " Просмотр всех пользователей"
            setOnClickListener {
                showAllUsers()
            }
        }

        val btnViewEnrollments = Button(this).apply {
            text = " Просмотр всех заявок"
            setOnClickListener {
                showAllEnrollments()
            }
        }

        // УБРАЛИ кнопку добавления тестовых пользователей

        val btnLogout = Button(this).apply {
            text = " Выйти из админ-аккаунта"
            setOnClickListener {
                logoutAdmin()
            }
        }

        val btnBack = Button(this).apply {
            text = "⬅ Назад к курсам"
            setOnClickListener {
                goBackToCourses()
            }
        }

        layout.addView(title)
        layout.addView(tvAdminInfo)
        layout.addView(btnViewUsers)
        layout.addView(btnViewEnrollments)
        layout.addView(btnLogout)
        layout.addView(btnBack)

        setContentView(layout)
    }

    private fun getAdminInfo(): String {
        val user = sharedPrefManager.getUser()

        return if (user != null) {
            "Вы вошли как администратор:\n" +
                    " ${user.name}\n" +
                    " ${user.email}"
        } else {
            "Вы не авторизованы"
        }
    }

    private fun showAllUsers() {
        Toast.makeText(this, "Загрузка пользователей...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                // Получаем ВСЕХ пользователей
                val allUsers = sharedPrefManager.getAllUsers()

                runOnUiThread {
                    if (allUsers.isNotEmpty()) {
                        showUsersDialog(allUsers)
                    } else {
                        Toast.makeText(this@AdminActivity, "Нет пользователей", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@AdminActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showUsersDialog(users: List<User>) {
        val usersText = users.joinToString("\n\n") { user ->
            val isAdmin = user.email == "admin@dpo.ru"
            " ${user.name}\n" +
                    " ${user.email}\n" +
                    "${if (user.phone.isNotBlank()) "📞 ${user.phone}\n" else ""}" +
                    " Записан на курсов: ${user.enrolledCourses.size}\n" +
                    "${if (isAdmin) "👑 АДМИНИСТРАТОР\n" else ""}" +
                    " ID: ${user.id.substring(0, minOf(15, user.id.length))}..."
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Все пользователи (${users.size})")
            .setMessage(usersText)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAllEnrollments() {
        Toast.makeText(this, "Загрузка заявок...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val enrollments = runBlocking {
                    try {
                        FirebaseRepository.getAllEnrollmentsForAdmin()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                runOnUiThread {
                    if (enrollments.isEmpty()) {
                        Toast.makeText(this@AdminActivity, "Заявок пока нет", Toast.LENGTH_SHORT).show()
                    } else {
                        showEnrollmentsDialog(enrollments)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@AdminActivity,
                        "Ошибка загрузки заявок",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun showEnrollmentsDialog(enrollments: List<Enrollment>) {
        val enrollmentsText = enrollments.take(10).joinToString("\n\n") { enrollment ->
            "👤 ${enrollment.userName}\n" +
                    "📧 ${enrollment.userEmail}\n" +
                    "📚 ${enrollment.courseTitle}\n" +
                    "📅 ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(enrollment.timestamp)}\n" +
                    "📊 Статус: ${enrollment.status}"
        }

        val moreText = if (enrollments.size > 10) "\n\n... и ещё ${enrollments.size - 10} заявок" else ""

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Заявки на курсы (${enrollments.size})")
            .setMessage(enrollmentsText + moreText)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun logoutAdmin() {
        sharedPrefManager.clearUser()

        Toast.makeText(
            this,
            " Вы вышли из админ-аккаунта",
            Toast.LENGTH_SHORT
        ).show()

        // Возвращаемся на главную
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun goBackToCourses() {
        finish()
    }
}