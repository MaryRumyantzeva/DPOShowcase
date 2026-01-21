package com.example.dposhowcase

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val title = TextView(this).apply {
            text = " Вход для администратора"
            textSize = 20f
            setPadding(0, 0, 0, 30)
        }

        val tvInstructions = TextView(this).apply {
            text = "Для демонстрации используйте:\n\n" +
                    " Email: admin@dpo.ru\n" +
                    " Пароль: admin123\n\n" +
                    "Или войдите как обычный пользователь:"
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }

        val etEmail = EditText(this).apply {
            hint = "Email администратора"
            setPadding(20, 20, 20, 20)
        }

        val etPassword = EditText(this).apply {
            hint = "Пароль"
            setPadding(20, 20, 20, 20)
        }

        val btnLoginAdmin = Button(this).apply {
            text = " Войти как администратор"
            setOnClickListener {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()

                if (email == "admin@dpo.ru" && password == "admin123") {
                    // Создаем админ-пользователя
                    val adminUser = User(
                        id = "admin_001",
                        name = "Администратор",
                        email = email,
                        phone = "+79990000000",
                        enrolledCourses = emptyList()
                    )

                    val sharedPrefManager = SharedPrefManager(this@AdminLoginActivity)
                    sharedPrefManager.addOrUpdateUser(adminUser)

                    Toast.makeText(this@AdminLoginActivity, "✅ Вход выполнен как администратор", Toast.LENGTH_SHORT).show()

                    // Переходим в админ-панель
                    startActivity(Intent(this@AdminLoginActivity, AdminActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@AdminLoginActivity, "❌ Неверные данные", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnLoginUser = Button(this).apply {
            text = " Войти как обычный пользователь"
            setOnClickListener {
                startActivity(Intent(this@AdminLoginActivity, ProfileActivity::class.java))
                finish()
            }
        }

        val btnBack = Button(this).apply {
            text = "⬅ Назад к курсам"
            setOnClickListener {
                startActivity(Intent(this@AdminLoginActivity, MainActivity::class.java))
                finish()
            }
        }

        layout.addView(title)
        layout.addView(tvInstructions)
        layout.addView(etEmail)
        layout.addView(etPassword)
        layout.addView(btnLoginAdmin)
        layout.addView(btnLoginUser)
        layout.addView(btnBack)

        setContentView(layout)

        // Автозаполнение для демонстрации
        etEmail.setText("admin@dpo.ru")
        etPassword.setText("admin123")
    }
}