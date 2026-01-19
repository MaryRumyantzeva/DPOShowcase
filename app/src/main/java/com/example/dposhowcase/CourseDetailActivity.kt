package com.example.dposhowcase

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class CourseDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COURSE = "course"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_detail)

        // Получаем объект курса
        val course = intent.getParcelableExtra<Course>(EXTRA_COURSE)

        if (course == null) {
            Toast.makeText(this, "Ошибка загрузки курса", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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

        // Кнопка записи
        findViewById<Button>(R.id.btnEnroll).setOnClickListener {
            showEnrollmentDialog(course)
        }

        // Кнопка копирования email - ИСПРАВЛЕНО!
        findViewById<Button>(R.id.btnCopyEmail).setOnClickListener {
            val clipboard = getSystemService(ClipboardManager::class.java)
            val clip = ClipData.newPlainText("Email преподавателя", course.contact_email)
            (clipboard as ClipboardManager).setPrimaryClip(clip)
            Toast.makeText(this, "Email скопирован: ${course.contact_email}", Toast.LENGTH_SHORT).show()
        }

        // Кнопка назад
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun showEnrollmentDialog(course: Course) {
        // Проверь, существует ли файл dialog_enrollment.xml!
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_enrollment, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Запись на курс: ${course.title}")
                .setView(dialogView)
                .setPositiveButton("Отправить заявку") { _, _ ->
                    val name = dialogView.findViewById<TextInputEditText>(R.id.etName)?.text?.toString() ?: ""
                    val email = dialogView.findViewById<TextInputEditText>(R.id.etEmail)?.text?.toString() ?: ""
                    val phone = dialogView.findViewById<TextInputEditText>(R.id.etPhone)?.text?.toString() ?: ""

                    if (name.isBlank() || email.isBlank()) {
                        Toast.makeText(this, "Заполните имя и email", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    saveEnrollment(course, name, email, phone)
                }
                .setNegativeButton("Отмена", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun saveEnrollment(course: Course, name: String, email: String, phone: String) {
        Toast.makeText(
            this,
            " Заявка отправлена!\n\n" +
                    "Курс: ${course.title}\n" +
                    "Ваше имя: $name\n" +
                    "Email: $email\n" +
                    "Телефон: ${if (phone.isBlank()) "не указан" else phone}\n\n" +
                    "Мы свяжемся с вами в течение 24 часов!",
            Toast.LENGTH_LONG
        ).show()
    }
}