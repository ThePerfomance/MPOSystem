package com.example.groupprojectfirsttry

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.api.AddUserToGroupRequest
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.ApiService
import com.example.groupprojectfirsttry.api.Group
import com.example.groupprojectfirsttry.api.LoginCredentials
import com.example.groupprojectfirsttry.simpleClasses.User
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {

    // ─── UI ──────────────────────────────────────────────────────────────────

    private lateinit var btnSignInApp: Button
    private lateinit var tvRegistration: TextView
    private lateinit var tvGoBack: TextView
    private lateinit var tvAboutProgramm: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var imgEye: ImageView

    private lateinit var etSurname: EditText
    private lateinit var etName: EditText
    private lateinit var etPatronymic: EditText
    private lateinit var etEmailReg: EditText
    private lateinit var etPasswordReg: EditText
    private lateinit var groupAutoComplete: MaterialAutoCompleteTextView
    private lateinit var btnRegistration: Button

    // ─── Data ─────────────────────────────────────────────────────────────────

    private lateinit var apiService: ApiService
    private var groupsList: List<Group> = emptyList()
    private var isPasswordVisible = false

    // Все вьюшки формы входа
    private val loginViews get() = listOf<View>(
        etEmail, etPassword, btnSignInApp, tvRegistration, imgEye
    )

    // Все вьюшки формы регистрации
    private val registerViews get() = listOf<View>(
        etSurname, etName, etPatronymic, etEmailReg,
        etPasswordReg, groupAutoComplete, btnRegistration, tvGoBack
    )

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupFullScreen()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        apiService = ApiClient.apiService

        initViews()
        setupListeners()
        loadGroups()
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun setupFullScreen() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun initViews() {
        btnSignInApp    = findViewById(R.id.buttonSignInApp)
        tvRegistration  = findViewById(R.id.textViewRegistration)
        tvGoBack        = findViewById(R.id.textViewGoBackSignUp)
        tvAboutProgramm = findViewById(R.id.textViewAboutApp)
        etEmail         = findViewById(R.id.editTextTextEmail)
        etPassword      = findViewById(R.id.editTextTextPassword)
        imgEye          = findViewById(R.id.eyeIcon)

        etSurname       = findViewById(R.id.editTextSurname)
        etName          = findViewById(R.id.editTextName)
        etPatronymic    = findViewById(R.id.editTextOtchestvo)
        etEmailReg      = findViewById(R.id.editTextTextEmailRegistration)
        etPasswordReg   = findViewById(R.id.editTextTextPasswordRegistration)
        groupAutoComplete = findViewById(R.id.groupAutoCompleteGroup)
        btnRegistration = findViewById(R.id.buttonRegistration)

        setupPasswordToggle()
        showLoginForm()
    }

    private fun setupListeners() {
        btnSignInApp.setOnClickListener {
            login(etEmail.text.toString(), etPassword.text.toString())
        }
        btnRegistration.setOnClickListener { register() }
        tvRegistration.setOnClickListener  { showRegistrationForm() }
        tvGoBack.setOnClickListener        { showLoginForm() }
        tvAboutProgramm.setOnClickListener { showAboutDialog() }
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading groups from api/groups/...")
                groupsList = apiService.getAllGroups()
                Log.d(TAG, "Groups loaded: ${groupsList.size}")
                
                val adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_list_item_1,
                    groupsList.map { it.name }
                )
                groupAutoComplete.setAdapter(adapter)
                groupAutoComplete.setOnClickListener { groupAutoComplete.showDropDown() }
                groupAutoComplete.setOnItemClickListener { parent, _, position, _ ->
                    val selected = parent.getItemAtPosition(position).toString()
                    toast("Выбрана группа: $selected")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load groups", e)
                if (e is HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e(TAG, "HTTP Error ${e.code()}: $errorBody")
                }
                toast("Не удалось загрузить группы: ${e.message}")
            }
        }
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────

    private fun login(email: String, password: String) {
        lifecycleScope.launch {
            try {
                when {
                    email.isEmpty() || password.isEmpty() ->
                        return@launch toast("Заполните все поля")
                    !isValidEmail(email) ->
                        return@launch toast("Некорректный формат email")
                }

                // Создаём объект с данными для отправки
                val credentials = LoginCredentials(email = email, password = password)

                // Отправляем запрос на сервер
                val response = apiService.authenticateUser(credentials)

                if (response.isSuccessful) {
                    // Сервер вернул 200 OK
                    val authenticatedUser = response.body()
                    if (authenticatedUser != null) {
                        toast("Вход выполнен!")
                        navigateToMainScreen(authenticatedUser)
                    } else {
                        toast("Ошибка: получен пустой ответ от сервера")
                    }
                } else {
                    // Сервер вернул ошибку (например, 401 Unauthorized)
                    when (response.code()) {
                        401 -> toast("Неверный email или пароль")
                        404 -> toast("Пользователь не найден")
                        else -> toast("Ошибка сервера: ${response.code()}. ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                toast("Ошибка: время ожидания истекло")
            } catch (e: java.net.UnknownHostException) {
                toast("Ошибка соединения: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                toast("Произошла ошибка: ${e.message}")
            }
        }
    }

    private fun register() {
        val surname    = etSurname.text.toString().trim()
        val name       = etName.text.toString().trim()
        val patronymic = etPatronymic.text.toString().trim()
        val group      = groupAutoComplete.text.toString().trim()
        val email      = etEmailReg.text.toString().trim()
        val password   = etPasswordReg.text.toString().trim()

        // Валидация — возвращаем при первой ошибке
        val validationError = validate(surname, name, patronymic, group, email, password)
        if (validationError != null) return toast(validationError)

        val newUser = User(
            firstname    = name,
            lastname     = surname,
            patronymic   = patronymic,
            email        = email,
            passwordHash = password,
            role         = "student"
        )

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Registering: $newUser")
                val response = apiService.registerUser(newUser)

                if (response.isSuccessful) {
                    toast("Регистрация прошла успешно!")
                    val user = apiService.getUserByEmail(email)
                    val selectedGroup = groupsList.find { it.name == group }

                    when {
                        selectedGroup == null -> toast("Группа не найдена")
                        user.id == null       -> toast("Ошибка: ID пользователя не найден")
                        else -> addUserToGroup(selectedGroup.id, user.id)
                    }
                    showLoginForm()
                } else {
                    toast(if (response.code() == 409) "Пользователь с таким email уже существует"
                    else "Ошибка регистрации: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Register error: ${e.message}", e)
                toast("Ошибка соединения: ${e.message}")
            }
        }
    }

    private fun addUserToGroup(groupId: java.util.UUID, userId: java.util.UUID) {
        lifecycleScope.launch {
            try {
                val response = apiService.addUserToGroup(
                    AddUserToGroupRequest(group_id = groupId, user_id = userId)
                )
                toast(
                    if (response.isSuccessful) "Пользователь добавлен в группу"
                    else "Ошибка добавления в группу: ${response.code()}"
                )
            } catch (e: Exception) {
                toast("Ошибка: ${e.message}")
            }
        }
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private fun validate(
        surname: String, name: String, patronymic: String,
        group: String, email: String, password: String
    ): String? {
        if (listOf(surname, name, patronymic, group, email, password).any { it.isEmpty() })
            return "Заполните все поля"

        val nameChecks = listOf(
            surname    to "Фамилия",
            name       to "Имя",
            patronymic to "Отчество"
        )
        for ((value, label) in nameChecks) {
            if (value.length < 2)          return "$label слишком короткое"
            if (!isValidName(value))       return "$label: только буквы, первая заглавная"
        }

        if (groupsList.none { it.name == group }) return "Выберите группу из списка"
        if (!isValidEmail(email))                 return "Некорректный формат email"
        if (!isValidPassword(password))           return "Пароль: минимум 6 символов, буква и цифра"

        return null // Всё ок
    }

    private fun isValidName(text: String) =
        Regex("^[А-ЯЁA-Z][а-яёa-zA-ZА-ЯЁ\\-]+\$").matches(text)

    private fun isValidEmail(email: String) =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isValidPassword(password: String) =
        Regex("^(?=.*[A-Za-zА-ЯЁа-яё])(?=.*\\d).{6,}\$").matches(password)

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private fun showLoginForm() = setVisibility(
        visible   = loginViews,
        invisible = registerViews
    )

    private fun showRegistrationForm() = setVisibility(
        visible   = registerViews,
        invisible = loginViews
    )

    // Один метод вместо двух с повторяющимися списками
    private fun setVisibility(visible: List<View>, invisible: List<View>) {
        visible.forEach   { it.visibility = View.VISIBLE }
        invisible.forEach { it.visibility = View.INVISIBLE }
    }

    private fun setupPasswordToggle() {
        etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        imgEye.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.inputType = if (isPasswordVisible)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            imgEye.setImageResource(
                if (isPasswordVisible) R.drawable.ic_visibility_on
                else R.drawable.ic_visibility_off
            )
            etPassword.setSelection(etPassword.text.length)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("О программе")
            .setMessage(getString(R.string.about_message))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun navigateToMainScreen(user: User) {
        startActivity(
            Intent(this, SecondActivityWithBottomNavMenu::class.java)
                .putExtra("user", user)
        )
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }

    // Расширение для краткого Toast
    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TAG = "MainActivity"
    }
}