package com.example.dposhowcase

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TestFirebaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_firebase)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnTest = findViewById<Button>(R.id.btnTest)

        btnTest.setOnClickListener {
            testFirebaseConnection(tvStatus)
        }
    }

    private fun testFirebaseConnection(tvStatus: TextView) {
        CoroutineScope(Dispatchers.Main).launch {
            tvStatus.text = "Проверка подключения..."

            try {
                val courses = withContext(Dispatchers.IO) {
                    FirebaseService.getCoursesFromFirebase()
                }

                tvStatus.text = "✅ Firebase подключен!\nКурсов в базе: ${courses.size}"

                if (courses.isEmpty()) {
                    withContext(Dispatchers.IO) {
                        FirebaseService.addSampleCourses()
                    }
                    tvStatus.text = "✅ Добавлены тестовые курсы"
                }

            } catch (e: Exception) {
                tvStatus.text = "❌ Ошибка Firebase: ${e.message}"
                Toast.makeText(this@TestFirebaseActivity, "Проверьте интернет и настройки Firebase", Toast.LENGTH_LONG).show()
            }
        }
    }
}