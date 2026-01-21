package com.example.dposhowcase

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SimpleAdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создаем простой интерфейс
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val tvTitle = TextView(this).apply {
            text = "⚙️ Панель администратора"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }

        val tvStatus = TextView(this).apply {
            text = "Статистика загружается..."
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }

        val btnShowUsers = Button(this).apply {
            text = " Показать всех пользователей"
            setOnClickListener { showAllUsers() }
        }

        val btnShowEnrollments = Button(this).apply {
            text = " Показать все заявки"
            setOnClickListener { showAllEnrollments() }
        }

        val btnTestData = Button(this).apply {
            text = " Добавить тестовые данные"
            setOnClickListener { addTestData() }
        }

        val btnBack = Button(this).apply {
            text = "Назад"
            setOnClickListener { finish() }
        }

        layout.addView(tvTitle)
        layout.addView(tvStatus)
        layout.addView(btnShowUsers)
        layout.addView(btnShowEnrollments)
        layout.addView(btnTestData)
        layout.addView(btnBack)

        setContentView(layout)

        // Загружаем статистику
        loadStatistics(tvStatus)
    }

    private fun loadStatistics(tvStatus: TextView) {
        val sharedPrefManager = SharedPrefManager(this)

        // Простая статистика (можно расширить)
        tvStatus.text = "Админ-панель готова к работе\n" +
                "Используйте кнопки ниже для управления"
    }

    private fun showAllUsers() {
        val sharedPrefManager = SharedPrefManager(this)

        // Получаем всех пользователей из SharedPreferences
        val allUsersJson = sharedPrefManager.getSharedPref().getString("all_users", "{}")

        // Исправлено: явно указываем тип для emptyMap()
        val usersMap: Map<String, Any> = try {
            com.google.gson.Gson().fromJson(allUsersJson, Map::class.java) as? Map<String, Any>
                ?: emptyMap<String, Any>()
        } catch (e: Exception) {
            emptyMap<String, Any>()
        }

        if (usersMap.isEmpty()) {
            Toast.makeText(this, "Пользователей пока нет", Toast.LENGTH_SHORT).show()
            return
        }

        val userList = mutableListOf<User>()
        usersMap.values.forEach { userJson ->
            try {
                val user = com.google.gson.Gson().fromJson(userJson.toString(), User::class.java)
                userList.add(user)
            } catch (e: Exception) {
                // Игнорируем некорректные записи
            }
        }

        if (userList.isEmpty()) {
            Toast.makeText(this, "Нет данных о пользователях", Toast.LENGTH_SHORT).show()
            return
        }

        val usersText = userList.joinToString("\n\n") { user ->
            "👤 ${user.name}\n" +
                    " ${user.email}\n" +
                    "${if (user.phone.isNotBlank()) "📞 ${user.phone}\n" else ""}" +
                    " Записан на курсов: ${user.enrolledCourses.size}\n" +
                    " ID: ${user.id}"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Все пользователи (${userList.size})")
            .setMessage(usersText)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAllEnrollments() {
        // Показываем заявки из Firebase или локально
        Toast.makeText(this, "Загрузка заявок...", Toast.LENGTH_SHORT).show()

        // В фоне загружаем из Firebase
        Thread {
            try {
                val enrollments = kotlinx.coroutines.runBlocking {
                    FirebaseRepository.getAllEnrollmentsForAdmin()
                }

                runOnUiThread {
                    if (enrollments.isEmpty()) {
                        MaterialAlertDialogBuilder(this@SimpleAdminActivity)
                            .setTitle("Заявки на курсы")
                            .setMessage("Заявок пока нет")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        val enrollmentsText = enrollments.joinToString("\n\n") { enrollment ->
                            "👤 ${enrollment.userName}\n" +
                                    "📧 ${enrollment.userEmail}\n" +
                                    "📚 ${enrollment.courseTitle}\n" +
                                    "📅 ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(enrollment.timestamp)}\n" +
                                    "📊 Статус: ${enrollment.status}"
                        }

                        MaterialAlertDialogBuilder(this@SimpleAdminActivity)
                            .setTitle("Заявки на курсы (${enrollments.size})")
                            .setMessage(enrollmentsText)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@SimpleAdminActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun addTestData() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Добавить тестовые данные")
            .setMessage("Что добавить?")
            .setPositiveButton("Тестовых пользователей") { _, _ ->
                addTestUsers()
            }
            .setNeutralButton("Тестовые заявки") { _, _ ->
                addTestEnrollments()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addTestUsers() {
        val sharedPrefManager = SharedPrefManager(this)

        val testUsers = listOf(
            User(
                id = "test_user_1",
                name = "Иван Иванов",
                email = "ivan@test.ru",
                phone = "+79991112233",
                enrolledCourses = listOf("1", "2")
            ),
            User(
                id = "test_user_2",
                name = "Мария Петрова",
                email = "maria@test.ru",
                phone = "+79994445566",
                enrolledCourses = listOf("3")
            ),
            User(
                id = "test_user_3",
                name = "Алексей Сидоров",
                email = "alex@test.ru",
                phone = "+79997778899",
                enrolledCourses = emptyList()
            )
        )

        testUsers.forEach { user ->
            sharedPrefManager.addOrUpdateUser(user)
        }

        Toast.makeText(this, "Добавлено 3 тестовых пользователя", Toast.LENGTH_SHORT).show()
    }

    private fun addTestEnrollments() {
        Toast.makeText(this, "Добавление тестовых заявок...", Toast.LENGTH_SHORT).show()

        // В фоне добавляем тестовые заявки в Firebase
        Thread {
            try {
                // Здесь можно добавить код для создания тестовых заявок в Firebase
                runOnUiThread {
                    Toast.makeText(this@SimpleAdminActivity, "Функция в разработке", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@SimpleAdminActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}

// Добавим расширение для доступа к SharedPreferences
fun SharedPrefManager.getSharedPref(): android.content.SharedPreferences {
    return this.javaClass.getDeclaredField("sharedPref").let {
        it.isAccessible = true
        it.get(this) as android.content.SharedPreferences
    }
}