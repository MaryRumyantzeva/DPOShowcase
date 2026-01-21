package com.example.dposhowcase

import android.content.Intent
import android.graphics.Color
import android.util.Log  // ДОБАВЬТЕ ЭТУ СТРОКУ!
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CoursesAdapter(
    private var courses: List<Course>,
    private val sharedPrefManager: SharedPrefManager,
    private val onItemClick: (Course) -> Unit
) : RecyclerView.Adapter<CoursesAdapter.CourseViewHolder>() {

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.courseTitle)
        val description: TextView = itemView.findViewById(R.id.courseDescription)
        val category: TextView = itemView.findViewById(R.id.courseCategory)
        val duration: TextView = itemView.findViewById(R.id.courseDuration)
        val price: TextView = itemView.findViewById(R.id.coursePrice)
        val enrollButton: Button = itemView.findViewById(R.id.enrollButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        val context = holder.itemView.context

        holder.itemView.setOnClickListener {
            onItemClick(course)
        }

        holder.title.text = course.title
        holder.description.text = course.description
        holder.category.text = course.category
        holder.duration.text = "Длительность: ${course.duration}"
        holder.price.text = course.getFormattedPrice()

        // Цвета для категорий
        val categoryColor = when (course.category) {
            "IT" -> Color.parseColor("#4CAF50")
            "Маркетинг" -> Color.parseColor("#2196F3")
            "Менеджмент" -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#9C27B0")
        }
        holder.category.setBackgroundColor(categoryColor)
        holder.category.setTextColor(Color.WHITE)

        // Обновляем кнопку
        updateEnrollButton(holder.enrollButton, course, context)

        // Кнопка записи
        holder.enrollButton.setOnClickListener {
            handleEnrollmentClick(context, course)
        }
    }

    private fun updateEnrollButton(button: Button, course: Course, context: android.content.Context) {
        val user = SharedPrefManager(context).getUser()

        if (user == null) {
            // Пользователь не авторизован
            button.text = "Записаться"
            button.isEnabled = true
            button.setBackgroundColor(Color.parseColor("#FF9800")) // Оранжевый
        } else if (user.hasEnrolledInCourse(course.id)) {
            // Пользователь уже записан на этот курс
            button.text = "✓ Записан"
            button.isEnabled = false
            button.setBackgroundColor(Color.parseColor("#4CAF50")) // Зеленый
        } else {
            // Пользователь авторизован, но не записан
            button.text = "Записаться"
            button.isEnabled = true
            button.setBackgroundColor(Color.parseColor("#2196F3")) // Синий
        }
        button.setTextColor(Color.WHITE)
    }

    private fun handleEnrollmentClick(context: android.content.Context, course: Course) {
        val user = SharedPrefManager(context).getUser()

        if (user == null) {
            // Неавторизованный пользователь
            MaterialAlertDialogBuilder(context)
                .setTitle("Требуется регистрация")
                .setMessage("Для записи на курс нужно один раз ввести ваши данные. После этого вы сможете записываться на любые курсы без повторного ввода.")
                .setPositiveButton("Ввести данные") { _, _ ->
                    // Переходим в профиль для регистрации
                    context.startActivity(Intent(context, ProfileActivity::class.java))
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else if (user.hasEnrolledInCourse(course.id)) {
            // Уже записан
            android.widget.Toast.makeText(
                context,
                "Вы уже записаны на этот курс!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            // Можно записываться
            showEnrollmentConfirmation(context, course, user)
        }
    }

    private fun showEnrollmentConfirmation(context: android.content.Context, course: Course, user: User) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Подтверждение записи")
            .setMessage("Вы действительно хотите записаться на курс:\n\n" +
                    "📚 ${course.title}\n" +
                    "💰 ${course.getFormattedPrice()}\n" +
                    "⏱ ${course.duration}")
            .setPositiveButton(" Да, записаться") { _, _ ->
                enrollUserToCourse(context, course, user)
            }
            .setNegativeButton(" Отмена", null)
            .show()
    }

    private fun enrollUserToCourse(context: android.content.Context, course: Course, user: User) {
        try {
            // 1. Обновляем локальные данные пользователя
            val updatedUser = user.copy(
                enrolledCourses = user.enrolledCourses + course.id
            )
            val sharedPrefManager = SharedPrefManager(context)
            sharedPrefManager.addOrUpdateUser(updatedUser) // Используем новый метод!

            // 2. Обновляем кнопку в текущем ViewHolder
            notifyDataSetChanged()

            // 3. Показываем уведомление
            android.widget.Toast.makeText(
                context,
                " Вы записались на курс: ${course.title}\n" +
                        "Данные сохранены локально.",
                android.widget.Toast.LENGTH_LONG
            ).show()

            // 4. Сохраняем в Firebase (в фоне, но не критично)
            saveEnrollmentToFirebase(context, course, updatedUser)

        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveEnrollmentToFirebase(context: android.content.Context, course: Course, user: User) {
        // Сохраняем в Firebase в фоновом потоке
        Thread {
            try {
                // Используем runBlocking для синхронного вызова
                kotlin.runCatching {
                    kotlinx.coroutines.runBlocking {
                        FirebaseRepository.saveEnrollmentToFirestore(course, user)
                    }
                }.onSuccess { success ->
                    if (success) {
                        Log.d("CoursesAdapter", "Успешно сохранено в Firebase") // Теперь Log распознается
                    }
                }.onFailure { e ->
                    Log.e("CoursesAdapter", "Ошибка Firebase", e)
                }
            } catch (e: Exception) {
                Log.e("CoursesAdapter", "Ошибка потока", e)
            }
        }.start()
    }

    // После успешной записи на курс:
    private fun syncUserWithFirebase(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseRepository.updateUserCourses(user.id, user.enrolledCourses)
            } catch (e: Exception) {
                Log.e("CourseDetail", "Ошибка синхронизации с Firebase", e)
            }
        }
    }

    override fun getItemCount(): Int = courses.size

    fun updateCourses(newCourses: List<Course>) {
        this.courses = newCourses
        notifyDataSetChanged()
    }
}