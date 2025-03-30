package com.example.groupprojectfirsttry

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.api.AddUserToGroupRequest
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.ApiService
import com.example.groupprojectfirsttry.api.Group
import com.example.groupprojectfirsttry.simpleClasses.User
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID


class MainActivity : AppCompatActivity() {
    //
    //UI
    //
    private lateinit var btnSignInApp:Button
    private lateinit var tvRegistration: TextView
    private lateinit var groupAutoComplete: MaterialAutoCompleteTextView
    private lateinit var etSurNameRegistration: EditText
    private lateinit var etNameRegistration: EditText
    private lateinit var etOtchestvoRegistration: EditText
    private lateinit var btnRegistration: Button
    private lateinit var etEmailRegistration: EditText
    private lateinit var etPasswordRegistration: EditText
    private lateinit var tvGoBack:TextView
    private lateinit var etPassword: EditText
    private lateinit var etEmail: EditText
    //
    //Server
    //
    private lateinit var apiService: ApiService
    //
    //Lists
    //
    private lateinit var GroupsList: List<Group>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        // Включаем полноэкранный режим
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )

        // Обработка системных инсетов
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Устанавливаем отступы, чтобы контент не перекрывался системными элементами
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            // Возвращаем инсеты для дальнейшей обработки
            insets
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        //
        ///////////////////////////////////////////////////
        //
        btnSignInApp = findViewById(R.id.buttonSignInApp)
        tvRegistration=findViewById(R.id.textViewRegistration)
        etEmail = findViewById(R.id.editTextTextEmail)
        etPassword = findViewById(R.id.editTextTextPassword)

        groupAutoComplete=findViewById(R.id.groupAutoCompleteGroup)
        etNameRegistration=findViewById(R.id.editTextName)
        etSurNameRegistration=findViewById(R.id.editTextSurname)
        etOtchestvoRegistration=findViewById(R.id.editTextOtchestvo)
        etEmailRegistration=findViewById(R.id.editTextTextEmailRegistration)
        etPasswordRegistration=findViewById(R.id.editTextTextPasswordRegistration)
        btnRegistration=findViewById(R.id.buttonRegistration)
        tvGoBack=findViewById(R.id.textViewGoBackSignUp)
        //
        ///////////////////////////////////////////////////
        //
        // Инициализация ApiService через ApiClient
        apiService = ApiClient.apiService

        // Обработчик кнопки входа
        btnSignInApp.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            login(email, password)
        }
        btnRegistration.setOnClickListener {
            register()
        }
        tvRegistration.setOnClickListener{

            showRegistrationForm()

        }
        tvGoBack.setOnClickListener {
            showLoginForm()
        }

        // Создаем список групп
        lifecycleScope.launch {
            try {
                val groups = apiService.getAllGroups()
                GroupsList=groups
                val groupNames = groups.map { it.name }.toTypedArray()
                // Set up the adapter with the fetched groups
                val adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_list_item_1, groupNames
                )
                groupAutoComplete.setAdapter(adapter)

                // Open the dropdown list on click
                groupAutoComplete.setOnClickListener { v -> groupAutoComplete.showDropDown() }

                // Handle item selection
                groupAutoComplete.setOnItemClickListener { parent, view, position, id ->
                    val selectedGroup: String = parent.getItemAtPosition(position).toString()
                    // Do something with the selected value
                    Toast.makeText(this@MainActivity, "Выбрана группа: $selectedGroup", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Не удалось загрузить группы", Toast.LENGTH_SHORT).show()
            }
        }
        //
        //Connection to Server TEST
        //
//        CoroutineScope(Dispatchers.IO).launch {
//            getRequest("http://192.168.31.249:3000/users")
//
//        }

    }
    //
    //Connection to Server TEST
    //
    private fun getRequest(url: String) {
        val client = OkHttpClient()
        Log.d("ffff","tgffffff")
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                println("Response: $responseBody")
                Log.d("Response",responseBody!!)
            } else {
                println("Request failed: ${response.code}")
                Log.d("Request failed", response.code.toString())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private fun login(email: String, password: String) {
        lifecycleScope.launch {
            try {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (!isValidEmail(email)) {
                    Toast.makeText(this@MainActivity, "Некорректный email", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val user = apiService.getUserByEmail(email)
                if (user.passwordHash == password) {
                    Toast.makeText(this@MainActivity, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                    navigateToMainScreen(user)
                } else {
                    Toast.makeText(this@MainActivity, "Неверный пароль", Toast.LENGTH_SHORT).show()
                }
            } catch (e: HttpException) {
                when (e.code()) {
                    404 -> Toast.makeText(this@MainActivity, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(this@MainActivity, "Ошибка: ${e.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun register() {
        val surname = etSurNameRegistration.text.toString().trim()
        val name = etNameRegistration.text.toString().trim()
        val patronymic = etOtchestvoRegistration.text.toString().trim()
        val group = groupAutoComplete.text.toString().trim()
        val email = etEmailRegistration.text.toString().trim()
        val password = etPasswordRegistration.text.toString().trim()

        if (surname.isEmpty() || name.isEmpty() ||
            patronymic.isEmpty() || group.isEmpty() ||
            email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Некорректный email", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        val newUser = User(
            firstname = name,
            lastname = surname,
            patronymic = patronymic,
            email = email,
            passwordHash = password,
            role = "student"
        )

        lifecycleScope.launch {
            try {
                Log.d("Register", "Starting registration for user: $newUser")
                val response = apiService.registerUser(newUser)
                if (response.isSuccessful) {
                    Log.d("Register", "Registration successful for user: $newUser")
                    Toast.makeText(this@MainActivity, "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show()

                    Log.d("Register", "Fetching user details by email: $email")
                    val user = apiService.getUserByEmail(email)

                    Log.d("Register", "User fetched: $user")

                    val selectedGroup = GroupsList.find { it.name == group }
                    if (selectedGroup != null) {
                        Log.d("Register", "Selected group found: $selectedGroup")
                        user.id?.let { userId ->
                            Log.d("Register", "Adding user with id: $userId to group with id: ${selectedGroup.id}")
                            addUserToGroup(selectedGroup.id, userId)
                        } ?: run {
                            Log.d("Register", "User ID is null")
                            Toast.makeText(this@MainActivity, "Ошибка: User ID is null", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d("Register", "Group not found: $group")
                        Toast.makeText(this@MainActivity, "Группа не найдена", Toast.LENGTH_SHORT).show()
                    }

                    showLoginForm()
                } else {
                    Log.d("Register", "Registration failed with code: ${response.code()}")
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка регистрации: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("Register", "Error during registration: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun addUserToGroup(groupId: UUID, userId: UUID) {
        lifecycleScope.launch {
            try {
                val addUserRequest = AddUserToGroupRequest(group_id = groupId, user_id = userId)
                val response = apiService.addUserToGroup(addUserRequest)
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Пользователь успешно добавлен в группу", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Ошибка при добавлении пользователя в группу: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showLoginForm() {
        // Поля входа: видимы
        findViewById<EditText>(R.id.editTextTextEmail).visibility = View.VISIBLE
        findViewById<EditText>(R.id.editTextTextPassword).visibility = View.VISIBLE
        findViewById<Button>(R.id.buttonSignInApp).visibility = View.VISIBLE
        findViewById<TextView>(R.id.textViewRegistration).visibility = View.VISIBLE

        // Поля регистрации: скрыты
        findViewById<EditText>(R.id.editTextSurname).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.editTextName).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.editTextOtchestvo).visibility = View.INVISIBLE
        findViewById<MaterialAutoCompleteTextView>(R.id.groupAutoCompleteGroup).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.editTextTextEmailRegistration).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.editTextTextPasswordRegistration).visibility = View.INVISIBLE
        findViewById<Button>(R.id.buttonRegistration).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.textViewGoBackSignUp).visibility = View.INVISIBLE
    }
    private fun showRegistrationForm() {
        // Поля входа: скрыты
        findViewById<EditText>(R.id.editTextTextEmail).visibility = View.INVISIBLE
        findViewById<EditText>(R.id.editTextTextPassword).visibility = View.INVISIBLE
        findViewById<Button>(R.id.buttonSignInApp).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.textViewRegistration).visibility = View.INVISIBLE

        // Поля регистрации: видимы
        findViewById<EditText>(R.id.editTextSurname).visibility = View.VISIBLE
        findViewById<EditText>(R.id.editTextName).visibility = View.VISIBLE
        findViewById<EditText>(R.id.editTextOtchestvo).visibility = View.VISIBLE
        findViewById<MaterialAutoCompleteTextView>(R.id.groupAutoCompleteGroup).visibility = View.VISIBLE
        findViewById<EditText>(R.id.editTextTextEmailRegistration).visibility = View.VISIBLE
        findViewById<EditText>(R.id.editTextTextPasswordRegistration).visibility = View.VISIBLE
        findViewById<Button>(R.id.buttonRegistration).visibility = View.VISIBLE
        findViewById<TextView>(R.id.textViewGoBackSignUp).visibility = View.VISIBLE
    }
    private fun navigateToMainScreen(user: User) {
        val intent = Intent(this, SecondActivityWithBottomNavMenu::class.java)
        intent.putExtra("user", user) // Передаем Parcelable
        startActivity(intent)
        finish()
    }
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}