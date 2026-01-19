package com.example.dposhowcase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class CoursesAdapter(
    private var courses: List<Course>,
    private val onEnrollClick: (Course) -> Unit
) : RecyclerView.Adapter<CoursesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.titleTextView)
        val category: TextView = view.findViewById(R.id.categoryTextView)
        val description: TextView = view.findViewById(R.id.descriptionTextView)
        val duration: TextView = view.findViewById(R.id.durationTextView)
        val price: TextView = view.findViewById(R.id.priceTextView)
        val enrollButton: Button = view.findViewById(R.id.enrollButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]

        holder.title.text = course.title
        holder.category.text = course.category
        holder.description.text = course.description
        holder.duration.text = "Длительность: ${course.duration}"
        holder.price.text = course.price

        holder.enrollButton.setOnClickListener {
            onEnrollClick(course)
        }
    }

    override fun getItemCount() = courses.size

    fun updateCourses(newCourses: List<Course>) {
        courses = newCourses
        notifyDataSetChanged()
    }
}