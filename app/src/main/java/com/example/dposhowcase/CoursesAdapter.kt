package com.example.dposhowcase

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog

class CoursesAdapter(
    private var courses: List<Course>,
    private val onItemClick: (Course) -> Unit
) : RecyclerView.Adapter<CoursesAdapter.CourseViewHolder>() {

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.courseTitle)
        val description: TextView = itemView.findViewById(R.id.courseDescription)
        val category: TextView = itemView.findViewById(R.id.courseCategory)
        val duration: TextView = itemView.findViewById(R.id.courseDuration)
        val price: TextView = itemView.findViewById(R.id.coursePrice)
        val enrollButton: Button = itemView.findViewById(R.id.enrollButton)

        init {
            itemView.setOnClickListener {
                val course = itemView.tag as? Course
                course?.let { courseItem ->
                    val context = itemView.context
                    val intent = Intent(context, CourseDetailActivity::class.java)
                    intent.putExtra(CourseDetailActivity.EXTRA_COURSE, courseItem) // Теперь работает!
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]

        // Сохраняем курс в тег для доступа при клике
        holder.itemView.tag = course

        holder.title.text = course.title
        holder.description.text = course.description
        holder.category.text = course.category
        holder.duration.text = "Длительность: ${course.duration}"
        holder.price.text = course.getFormattedPrice()

        // Цвета для категорий
        val categoryColor = when (course.category) {
            "IT" -> Color.parseColor("#4CAF50") // Зеленый
            "Маркетинг" -> Color.parseColor("#2196F3") // Синий
            "Менеджмент" -> Color.parseColor("#FF9800") // Оранжевый
            else -> Color.parseColor("#9C27B0") // Фиолетовый
        }
        holder.category.setBackgroundColor(categoryColor)
        holder.category.setTextColor(Color.WHITE)

        // Кнопка записи открывает диалог
        holder.enrollButton.setOnClickListener {
            showEnrollmentDialog(holder.itemView.context, course)
        }
    }

    private fun showEnrollmentDialog(context: android.content.Context, course: Course) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_enrollment, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Запись на курс")
            .setView(dialogView)
            .setPositiveButton("Отправить") { _, _ ->
                val name = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName).text.toString()
                val email = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail).text.toString()
                val phone = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPhone).text.toString()

                if (name.isBlank() || email.isBlank()) {
                    android.widget.Toast.makeText(context, "Заполните имя и email", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                sendEnrollmentRequest(context, course, name, email, phone)
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun sendEnrollmentRequest(context: android.content.Context, course: Course, name: String, email: String, phone: String) {
        // TODO: Отправка в Firebase
        android.widget.Toast.makeText(
            context,
            "Заявка отправлена!\n\nКурс: ${course.title}\nИмя: $name\nEmail: $email",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    override fun getItemCount(): Int = courses.size

    fun updateCourses(newCourses: List<Course>) {
        this.courses = newCourses
        notifyDataSetChanged()
    }
}