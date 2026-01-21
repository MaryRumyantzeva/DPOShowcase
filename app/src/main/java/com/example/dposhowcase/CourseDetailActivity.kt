package com.example.dposhowcase

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourseDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COURSE = "course"
    }

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var currentCourse: Course

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_detail)

        sharedPrefManager = SharedPrefManager(this)

        val course = intent.getParcelableExtra<Course>(EXTRA_COURSE)

        if (course == null) {
            Toast.makeText(this, "Ошибка загрузки курса", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentCourse = course

        // Заполняем данные
        findViewById<TextView>(R.id.tvCourseTitle).text = course.title
        findViewById<TextView>(R.id.tvInstructor).text = "Преподаватель: ${course.instructor}"
        findViewById<TextView>(R.id.tvDuration).text = "Длительность: ${course.duration}"
        findViewById<TextView>(R.id.tvHours).text = "Кол-во часов: ${course.hours}"
        findViewById<TextView>(R.id.tvPrice).text = "Цена: ${course.getFormattedPrice()}"
        findViewById<TextView>(R.id.tvDescription).text = course.description

        // Программа курса
        val syllabusText = course.syllabus.joinToString("\n• ", "• ")
        findViewById<TextView>(R.id.tvSyllabus).text = syllabusText

        // Требования
        val requirementsText = course.requirements.joinToString("\n• ", "• ")
        findViewById<TextView>(R.id.tvRequirements).text = requirementsText

        // Обновляем кнопку записи
        updateEnrollButton()

        // Кнопка записи
        findViewById<Button>(R.id.btnEnroll).setOnClickListener {
            checkAndEnroll()
        }

        // Кнопка копирования email
        findViewById<Button>(R.id.btnCopyEmail).setOnClickListener {
            val clipboard = getSystemService(ClipboardManager::class.java)
            val clip = ClipData.newPlainText("Email преподавателя", course.contact_email)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Email скопирован: ${course.contact_email}", Toast.LENGTH_SHORT).show()
        }

        // Кнопка назад
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем кнопку при возвращении на экран
        updateEnrollButton()
    }

    private fun updateEnrollButton() {
        val enrollButton = findViewById<Button>(R.id.btnEnroll)
        val user = sharedPrefManager.getUser()

        if (user == null) {
            // Пользователь не авторизован
            enrollButton.text = "Войти и записаться"
            enrollButton.isEnabled = true
            enrollButton.setBackgroundColor(Color.parseColor("#FF9800")) // Оранжевый
            enrollButton.setTextColor(Color.WHITE)
        } else if (user.hasEnrolledInCourse(currentCourse.id)) {
            // Пользователь уже записан на этот курс
            enrollButton.text = "✓ Вы уже записаны"
            enrollButton.isEnabled = false
            enrollButton.setBackgroundColor(Color.parseColor("#4CAF50")) // Зеленый
            enrollButton.setTextColor(Color.WHITE)
        } else {
            // Пользователь авторизован, но не записан
            enrollButton.text = "Записаться на курс"
            enrollButton.isEnabled = true
            enrollButton.setBackgroundColor(Color.parseColor("#2196F3")) // Синий
            enrollButton.setTextColor(Color.WHITE)
        }
    }

    private fun checkAndEnroll() {
        val user = sharedPrefManager.getUser()

        if (user == null) {
            // Неавторизованный пользователь
            AlertDialog.Builder(this)
                .setTitle("Требуется регистрация")
                .setMessage("Для записи на курс нужно один раз ввести ваши данные. После этого вы сможете записываться на любые курсы без повторного ввода.")
                .setPositiveButton("Ввести данные") { _, _ ->
                    // Переходим в профиль для регистрации
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else if (user.hasEnrolledInCourse(currentCourse.id)) {
            // Уже записан
            Toast.makeText(this, "Вы уже записаны на этот курс!", Toast.LENGTH_SHORT).show()
        } else {
            // Можно записываться
            showEnrollmentConfirmation(user)
        }
    }

    private fun showEnrollmentConfirmation(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Подтверждение записи")
            .setMessage("Вы действительно хотите записаться на курс:\n\n" +
                    "📚 ${currentCourse.title}\n" +
                    "💰 ${currentCourse.getFormattedPrice()}\n" +
                    "⏱ ${currentCourse.duration}\n\n" +
                    "Ваши данные:\n" +
                    "👤 ${user.name}\n" +
                    "📧 ${user.email}\n" +
                    "${if (user.phone.isNotBlank()) "📞 ${user.phone}\n" else ""}")
            .setPositiveButton("✅ Да, записаться") { _, _ ->
                enrollUserToCourse(user)
            }
            .setNegativeButton("❌ Отмена", null)
            .show()
    }

    private fun enrollUserToCourse(user: User) {
        try {
            // 1. Обновляем локальные данные пользователя
            val updatedUser = user.copy(
                enrolledCourses = user.enrolledCourses + currentCourse.id
            )
            sharedPrefManager.saveUser(updatedUser)

            // 2. Обновляем кнопку
            updateEnrollButton()

            // 3. Показываем уведомление
            Toast.makeText(
                this,
                "✅ Вы успешно записались на курс!\n\n" +
                        "Курс: ${currentCourse.title}\n" +
                        "На ваш email (${user.email}) отправлено подтверждение.",
                Toast.LENGTH_LONG
            ).show()

            // 4. Сохраняем заявку в Firebase
            saveEnrollmentToFirebase(user)

            // 5. Синхронизируем данные пользователя с Firebase (ДОБАВЬТЕ ЭТОТ ВЫЗОВ ЗДЕСЬ!)
            syncUserWithFirebase(updatedUser)

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncUserWithFirebase(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseRepository.updateUserCourses(user.id, user.enrolledCourses)
            } catch (e: Exception) {
                android.util.Log.e("CourseDetail", "Ошибка синхронизации с Firebase", e)
            }
        }
    }

    private fun saveEnrollmentToFirebase(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = FirebaseRepository.saveEnrollmentToFirestore(currentCourse, user)

                runOnUiThread {
                    if (success) {
                        // Дополнительное сообщение о сохранении в БД
                        Toast.makeText(
                            this@CourseDetailActivity,
                            "✅ Заявка сохранена в базе данных",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                // Не показываем ошибку пользователю, просто логируем
                android.util.Log.e("CourseDetail", "Firebase error", e)
            }
        }
    }}