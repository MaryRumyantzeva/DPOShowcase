package com.example.dposhowcase

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var coursesAdapter: CoursesAdapter
    private lateinit var categoriesContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private var allCourses = mutableListOf<Course>()
    private var selectedCategory = "Все"
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefManager = SharedPrefManager(this)

        // ВАЖНО: Очищаем текущего пользователя при запуске
        // чтобы никто не был залогинен по умолчанию
        sharedPrefManager.clearUser()

        // НАСТРОЙКА КНОПКИ ПРОФИЛЯ
        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            checkUserStatusAndNavigate()
        }

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

    // Проверка статуса пользователя и навигация
    private fun checkUserStatusAndNavigate() {
        val user = sharedPrefManager.getUser()

        if (user == null) {
            // Никто не залогинен - показываем выбор входа
            showLoginChoiceDialog()
        } else if (user.email == "admin@dpo.ru") {
            // Админ - открываем админку
            startActivity(Intent(this, AdminActivity::class.java))
        } else {
            // Обычный пользователь - открываем профиль
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
        }
    }

    // Диалог выбора входа (когда никто не залогинен)
    private fun showLoginChoiceDialog() {
        val options = arrayOf("👤 Войти как пользователь", "🔐 Войти как администратор")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Вход в систему")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showUserLoginDialog()
                    1 -> showAdminLoginDialog()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Диалог входа для обычного пользователя - ИСПРАВЛЕННЫЙ
    // Диалог входа для обычного пользователя - ИСПРАВЛЕННЫЙ (без автозаполнения)
    private fun showUserLoginDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_login, null)

        // УБРАЛИ автозаполнение тестовыми данными
        // Теперь поля будут пустыми

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("👤 Вход как пользователь")
            .setMessage("Введите ваши данные для входа")
            .setView(dialogView)
            .setPositiveButton("Войти") { _, _ ->
                val email = dialogView.findViewById<EditText>(R.id.etUserEmail)?.text?.toString()?.trim() ?: ""
                val name = dialogView.findViewById<EditText>(R.id.etUserName)?.text?.toString()?.trim() ?: ""
                val phone = dialogView.findViewById<EditText>(R.id.etUserPhone)?.text?.toString()?.trim() ?: ""

                if (email.isNotEmpty() && name.isNotEmpty()) {
                    // Проверяем email на валидность
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "❌ Введите корректный email", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Всегда создаем/обновляем и сохраняем пользователя
                    val user = User(
                        id = "user_${System.currentTimeMillis()}",
                        name = name,
                        email = email,
                        phone = phone,
                        enrolledCourses = emptyList()
                    )

                    // Сохраняем в общий список
                    sharedPrefManager.addOrUpdateUser(user)
                    // Сохраняем как текущего пользователя
                    sharedPrefManager.saveUser(user)

                    Toast.makeText(this, "✅ Добро пожаловать, ${user.name}!", Toast.LENGTH_SHORT).show()

                    // Обновляем статус кнопки профиля
                    updateProfileButton()

                    // Переходим в профиль
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("user", user)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "❌ Введите email и имя", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Диалог входа для администратора
    private fun showAdminLoginDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_login, null)

        val adminEmail = "admin@dpo.ru"
        val adminPassword = "admin123"

        // Проверяем, есть ли админ
        val adminUserId = sharedPrefManager.findUserIdByEmail(adminEmail)
        if (adminUserId == null) {
            // Создаем админа при первом входе
            val adminUser = User(
                id = "admin_${System.currentTimeMillis()}",
                name = "Администратор Системы",
                email = adminEmail,
                phone = "+79990000000",
                enrolledCourses = emptyList()
            )
            sharedPrefManager.addOrUpdateUser(adminUser)
            Log.d("MainActivity", "Создан администратор")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(" Вход для администратора")
            .setView(dialogView)
            .setPositiveButton("Войти") { _, _ ->
                val email = dialogView.findViewById<EditText>(R.id.etAdminEmail)?.text?.toString()?.trim() ?: ""
                val password = dialogView.findViewById<EditText>(R.id.etAdminPassword)?.text?.toString()?.trim() ?: ""

                if (email == adminEmail && password == adminPassword) {
                    val userId = sharedPrefManager.findUserIdByEmail(adminEmail)
                    if (userId != null) {
                        val adminUser = sharedPrefManager.getUserById(userId)
                        if (adminUser != null) {
                            sharedPrefManager.saveUser(adminUser)
                            Toast.makeText(this, "✅ Вход выполнен как администратор", Toast.LENGTH_SHORT).show()
                            updateProfileButton()
                            startActivity(Intent(this, AdminActivity::class.java))
                        }
                    }
                } else {
                    Toast.makeText(this, "❌ Неверные данные", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Обновление отображения кнопки профиля
    private fun updateProfileButton() {
        val user = sharedPrefManager.getUser()
        if (user != null) {
            // Можно обновить иконку или текст кнопки
            Toast.makeText(this, "Вы вошли как: ${user.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возвращении на экран
        if (::coursesAdapter.isInitialized) {
            coursesAdapter.notifyDataSetChanged()
        }

        // Проверяем статус пользователя
        val user = sharedPrefManager.getUser()
        if (user == null) {
            // Показываем подсказку, что нужно войти
            Toast.makeText(this, "Войдите в систему для записи на курсы", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeApp() {
        // 1. Инициализация поиска
        searchEditText = findViewById(R.id.searchEditText)

        // 2. Настройка списка курсов
        val recyclerView = findViewById<RecyclerView>(R.id.coursesRecyclerView)
        coursesAdapter = CoursesAdapter(emptyList(), sharedPrefManager) { course ->
            // Проверяем, залогинен ли пользователь
            val user = sharedPrefManager.getUser()
            if (user == null) {
                // Если не залогинен, просим войти
                showLoginChoiceDialog()
            } else {
                // Если залогинен, переходим на детальную страницу
                val intent = Intent(this, CourseDetailActivity::class.java)
                intent.putExtra(CourseDetailActivity.EXTRA_COURSE, course)
                intent.putExtra("current_user", user)
                startActivity(intent)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = coursesAdapter

        // 3. Настройка поиска
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterCourses(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 4. Настройка категорий
        categoriesContainer = findViewById(R.id.categoriesContainer)
        setupCategories()

        // 5. Загрузка курсов
        loadCourses()
    }

    private fun loadCourses() {
        CoroutineScope(Dispatchers.Main).launch {
            // Показываем статус загрузки
            findViewById<TextView>(R.id.tvStatus).apply {
                text = "Загрузка курсов..."
                visibility = View.VISIBLE
            }

            try {
                // Пробуем загрузить из Firebase
                val firestoreCourses = withContext(Dispatchers.IO) {
                    FirebaseRepository.getCoursesFromFirestore()
                }

                if (firestoreCourses.isNotEmpty()) {
                    allCourses = firestoreCourses.toMutableList()
                    Toast.makeText(
                        this@MainActivity,
                        "Загружено ${allCourses.size} курсов",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Если пусто, добавляем тестовые
                    withContext(Dispatchers.IO) {
                        FirebaseRepository.addSampleCoursesIfNeeded()
                    }

                    // Пробуем снова
                    val retryCourses = withContext(Dispatchers.IO) {
                        FirebaseRepository.getCoursesFromFirestore()
                    }

                    if (retryCourses.isNotEmpty()) {
                        allCourses = retryCourses.toMutableList()
                    } else {
                        // Используем локальные
                        loadLocalCourses()
                    }
                }

                // Обновляем адаптер
                coursesAdapter.updateCourses(allCourses)

                // Скрываем статус
                findViewById<TextView>(R.id.tvStatus).visibility = View.GONE

                // Применяем фильтр
                filterCourses(searchEditText.text.toString())

            } catch (e: Exception) {
                findViewById<TextView>(R.id.tvStatus).text = "Ошибка загрузки"
                Toast.makeText(
                    this@MainActivity,
                    "Ошибка загрузки курсов",
                    Toast.LENGTH_SHORT
                ).show()

                // Используем локальные данные
                loadLocalCourses()
                coursesAdapter.updateCourses(allCourses)
                findViewById<TextView>(R.id.tvStatus).visibility = View.GONE
            }
        }
    }

    private fun loadLocalCourses() {
        allCourses = mutableListOf(
            Course(
                id = "1",
                title = "Цифровой маркетинг",
                description = "Освойте инструменты интернет-продвижения",
                category = "Маркетинг",
                duration = "3 месяца",
                price = 15000.0,
                instructor = "Анна Петрова",
                hours = 72,
                syllabus = listOf("SEO", "Контекстная реклама", "SMM"),
                requirements = listOf("Базовые знания интернета"),
                contact_email = "marketing@dpo.ru"
            ),
            Course(
                id = "2",
                title = "Анализ данных на Python",
                description = "Научитесь работать с большими данными",
                category = "IT",
                duration = "4 месяца",
                price = 20000.0,
                instructor = "Иван Сидоров",
                hours = 96,
                syllabus = listOf("Python", "Pandas", "NumPy"),
                requirements = listOf("Базовые знания математики"),
                contact_email = "data@dpo.ru"
            ),
            Course(
                id = "3",
                title = "Управление проектами",
                description = "Освойте методики Agile и Scrum",
                category = "Менеджмент",
                duration = "2 месяца",
                price = 12000.0,
                instructor = "Мария Иванова",
                hours = 48,
                syllabus = listOf("Agile", "Scrum", "Управление рисками"),
                requirements = listOf("Опыт работы в команде"),
                contact_email = "pm@dpo.ru"
            )
        )
    }

    private fun setupCategories() {
        val categories = listOf("Все", "IT", "Маркетинг", "Менеджмент", "Дизайн", "Финансы", "Языки")

        categoriesContainer.removeAllViews()

        categories.forEach { category ->
            val button = Button(this).apply {
                text = category
                setPadding(32, 16, 32, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 8
                    bottomMargin = 4
                    topMargin = 4
                }

                // Стили для кнопок
                if (category == selectedCategory) {
                    setBackgroundColor(Color.parseColor("#6200EE"))
                } else {
                    setBackgroundColor(Color.parseColor("#757575"))
                }
                setTextColor(Color.WHITE)
                textSize = 14f
                isAllCaps = false

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
                button.setBackgroundColor(Color.parseColor("#6200EE"))
                button.setPadding(32, 16, 32, 16)
            } else {
                button.setBackgroundColor(Color.parseColor("#757575"))
                button.setPadding(32, 16, 32, 16)
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
}