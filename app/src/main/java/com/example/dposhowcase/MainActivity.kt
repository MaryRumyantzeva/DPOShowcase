package com.example.dposhowcase

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import android.view.View
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var coursesAdapter: CoursesAdapter
    private lateinit var categoriesContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private var allCourses = mutableListOf<Course>()
    private var selectedCategory = "Все"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Показываем что приложение запускается
        Toast.makeText(this, "Запуск приложения...", Toast.LENGTH_SHORT).show()

        // ОТЛОЖЕННАЯ ИНИЦИАЛИЗАЦИЯ
        findViewById<View>(android.R.id.content).postDelayed({
            try {
                initializeApp()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }, 500)
    }

    private fun initializeApp() {
        // 1. Инициализация поиска
        searchEditText = findViewById(R.id.searchEditText)

        // 2. Загрузка данных курсов (теперь с полной информацией)
        initCourses()

        // 3. Настройка списка курсов
        val recyclerView = findViewById<RecyclerView>(R.id.coursesRecyclerView)
        coursesAdapter = CoursesAdapter(allCourses) { course ->
            // При клике на курс открываем детальную страницу
            val intent = Intent(this, CourseDetailActivity::class.java)
            intent.putExtra(CourseDetailActivity.EXTRA_COURSE, course)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = coursesAdapter

        // 4. Настройка поиска
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterCourses(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 5. Настройка категорий
        categoriesContainer = findViewById(R.id.categoriesContainer)
        setupCategories()

        // 6. Успешная загрузка
        Toast.makeText(this, "Приложение загружено! Курсов: ${allCourses.size}", Toast.LENGTH_SHORT).show()
    }

    private fun initCourses() {
        allCourses = mutableListOf(
            Course(
                id = "1",
                title = "Цифровой маркетинг",
                description = "Освойте инструменты интернет-продвижения: SEO, контекстная реклама, SMM, email-маркетинг. Научитесь создавать эффективные рекламные кампании и анализировать их результаты.",
                category = "Маркетинг",
                duration = "3 месяца",
                price = 15000.0,
                instructor = "Анна Петрова",
                hours = 72,
                syllabus = listOf(
                    "Введение в цифровой маркетинг",
                    "SEO-оптимизация",
                    "Контекстная реклама",
                    "Социальные сети",
                    "Email-маркетинг",
                    "Аналитика"
                ),
                requirements = listOf(
                    "Базовые знания интернета",
                    "Умение работать с ПК"
                ),
                contact_email = "marketing@dpo.ru"
            ),
            Course(
                id = "2",
                title = "Анализ данных на Python",
                description = "Научитесь работать с большими данными, строить предсказательные модели и визуализировать результаты. Практика на реальных кейсах.",
                category = "IT",
                duration = "4 месяца",
                price = 20000.0,
                instructor = "Иван Сидоров",
                hours = 96,
                syllabus = listOf(
                    "Основы Python",
                    "Библиотеки Pandas и NumPy",
                    "Визуализация данных",
                    "Машинное обучение",
                    "Работа с базами данных",
                    "Реальные проекты"
                ),
                requirements = listOf(
                    "Базовые знания математики",
                    "Логическое мышление"
                ),
                contact_email = "data@dpo.ru"
            ),
            Course(
                id = "3",
                title = "Управление проектами",
                description = "Освойте методики Agile и Scrum, научитесь планировать проекты, управлять командой и контролировать бюджет.",
                category = "Менеджмент",
                duration = "2 месяца",
                price = 12000.0,
                instructor = "Мария Иванова",
                hours = 48,
                syllabus = listOf(
                    "Основы управления проектами",
                    "Методологии Agile и Scrum",
                    "Планирование и оценка",
                    "Управление рисками",
                    "Работа в команде",
                    "Инструменты управления"
                ),
                requirements = listOf(
                    "Опыт работы в команде",
                    "Базовые знания менеджмента"
                ),
                contact_email = "pm@dpo.ru"
            )
            // Добавь остальные курсы по аналогии
        )
    }

    private fun setupCategories() {
        val categories = listOf("Все", "IT", "Маркетинг", "Менеджмент")

        categories.forEach { category ->
            val button = Button(this).apply {
                text = category
                setPadding(32, 16, 32, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 16
                }

                // Простые цвета
                if (category == selectedCategory) {
                    setBackgroundColor(Color.parseColor("#FF6200EE")) // Фиолетовый
                } else {
                    setBackgroundColor(Color.parseColor("#757575")) // Серый
                }
                setTextColor(Color.WHITE)

                setOnClickListener {
                    selectedCategory = category
                    filterCourses(searchEditText.text.toString())
                    updateCategoryButtons()
                }
            }

            categoriesContainer.addView(button)
        }
    }

    private fun updateCategoryButtons() {
        for (i in 0 until categoriesContainer.childCount) {
            val button = categoriesContainer.getChildAt(i) as Button
            val buttonCategory = button.text.toString()

            if (buttonCategory == selectedCategory) {
                button.setBackgroundColor(Color.parseColor("#FF6200EE"))
            } else {
                button.setBackgroundColor(Color.parseColor("#757575"))
            }
        }
    }

    private fun filterCourses(searchText: String) {
        val filtered = allCourses.filter { course ->
            val matchesSearch = searchText.isEmpty() ||
                    course.title.contains(searchText, true) ||
                    course.description.contains(searchText, true)

            val matchesCategory = selectedCategory == "Все" ||
                    course.category == selectedCategory

            matchesSearch && matchesCategory
        }

        coursesAdapter.updateCourses(filtered)
    }

    private fun showEnrollmentDialog(course: Course) {
        Toast.makeText(
            this,
            "Заявка на курс отправлена!\n\n" +
                    "Курс: ${course.title}\n" +
                    "Категория: ${course.category}\n" +
                    "Длительность: ${course.duration}\n" +
                    "Цена: ${course.getFormattedPrice()}",
            Toast.LENGTH_LONG
        ).show()
    }
}