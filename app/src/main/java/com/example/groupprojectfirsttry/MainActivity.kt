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
import com.example.groupprojectfirsttry.api.*
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
    
    private lateinit var loadingProgressBar: ProgressBar

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

        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        setupFullScreen()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        ApiClient.init(this)
        apiService = ApiClient.apiService

        initViews()
        setupListeners()
        
        // Пытаемся выполнить авто-вход
        val tm = ApiClient.getTokenManager()
        val token = tm?.getAccessToken()
        val email = tm?.getUserEmail()

        if (token != null && email != null) {
            performAutoLogin(email)
        } else {
            loadGroups()
            showLoginForm()
        }
    }

    private fun performAutoLogin(email: String) {
        Log.d(TAG, "Attempting auto-login for $email")
        setLoading(true)
        
        lifecycleScope.launch {
            try {
                val user = apiService.getUserByEmail(email)
                Log.d(TAG, "Auto-login successful for ${user.firstname}")
                navigateToMainScreen(user)
            } catch (e: Exception) {
                Log.e(TAG, "Auto-login failed: ${e.message}")
                setLoading(false)
                loadGroups()
                showLoginForm()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        findViewById<View>(R.id.main)?.alpha = if (isLoading) 0.2f else 1.0f
        
        // Блокируем клики во время загрузки
        btnSignInApp.isEnabled = !isLoading
        tvRegistration.isEnabled = !isLoading
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
                groupsList = apiService.getAllGroups()
                val adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    groupsList.map { it.name }
                )
                groupAutoComplete.setAdapter(adapter)
                
                // Чтобы список открывался при клике
                groupAutoComplete.setOnClickListener {
                    groupAutoComplete.showDropDown()
                }
                groupAutoComplete.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) groupAutoComplete.showDropDown()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load groups", e)
            }
        }
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────

    private fun login(email: String, password: String) {
        lifecycleScope.launch {
            try {
                if (email.isEmpty() || password.isEmpty()) return@launch toast("Заполните все поля")
                
                setLoading(true)
                val credentials = LoginCredentials(email = email, password = password)
                val response = apiService.authenticateUser(credentials)

                if (response.isSuccessful) {
                    val tokenResponse = response.body()
                    if (tokenResponse != null) {
                        val tm = TokenManager(this@MainActivity)
                        tm.saveTokens(tokenResponse.access, tokenResponse.refresh)
                        tm.saveUserEmail(email)

                        val user = apiService.getUserByEmail(email)
                        navigateToMainScreen(user)
                    } else {
                        setLoading(false)
                        toast("Ошибка сервера")
                    }
                } else {
                    setLoading(false)
                    toast("Неверный логин или пароль")
                }
            } catch (e: Exception) {
                setLoading(false)
                toast("Ошибка соединения: ${e.message}")
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

        if (listOf(surname, name, patronymic, group, email, password).any { it.isEmpty() }) 
            return toast("Заполните все поля")

        val newUser = User(
            firstname    = name,
            lastname     = surname,
            patronymic   = patronymic,
            username     = email,
            email        = email,
            password     = password,
            role         = "student"
        )

        lifecycleScope.launch {
            try {
                setLoading(true)
                val response = apiService.registerUser(newUser)
                if (response.isSuccessful) {
                    val user = apiService.getUserByEmail(email)
                    val selectedGroup = groupsList.find { it.name == group }
                    if (selectedGroup != null && user.id != null) {
                        apiService.addUserToGroup(AddUserToGroupRequest(group_id = selectedGroup.id, user_id = user.id))
                    }
                    setLoading(false)
                    showLoginForm()
                    toast("Регистрация успешна")
                } else {
                    setLoading(false)
                    toast("Ошибка регистрации")
                }
            } catch (e: Exception) {
                setLoading(false)
                toast("Ошибка соединения")
            }
        }
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private fun showLoginForm() = setVisibility(visible = loginViews, invisible = registerViews)
    private fun showRegistrationForm() = setVisibility(visible = registerViews, invisible = loginViews)

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
            etPassword.setSelection(etPassword.text.length)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this).setTitle("О программе").setMessage("MPOSystem v1.0").show()
    }

    private fun navigateToMainScreen(user: User) {
        startActivity(Intent(this, SecondActivityWithBottomNavMenu::class.java).putExtra("user", user))
        finish()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TAG = "MainActivity"
    }
}
