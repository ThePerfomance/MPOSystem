package com.example.groupprojectfirsttry

import android.R.attr.value
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
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class MainActivity : AppCompatActivity() {

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
        etEmail=findViewById(R.id.editTextTextPassword)
        etPassword=findViewById(R.id.editTextTextEmail)

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
        btnSignInApp.setOnClickListener {
            val intent = Intent(this, SecondActivityWithBottomNavMenu::class.java)
            intent.putExtra("key", value)
            startActivity(intent)
        }
        tvRegistration.setOnClickListener{

            btnSignInApp.visibility=View.INVISIBLE
            etPassword.visibility=View.INVISIBLE
            etEmail.visibility=View.INVISIBLE
            tvRegistration.visibility=View.INVISIBLE

            groupAutoComplete.visibility=View.VISIBLE
            etNameRegistration.visibility=View.VISIBLE
            etSurNameRegistration.visibility=View.VISIBLE
            etOtchestvoRegistration.visibility=View.VISIBLE
            etEmailRegistration.visibility=View.VISIBLE
            etPasswordRegistration.visibility=View.VISIBLE
            btnRegistration.visibility=View.VISIBLE
            tvGoBack.visibility=View.VISIBLE

        }
        tvGoBack.setOnClickListener {
            btnSignInApp.visibility=View.VISIBLE
            etPassword.visibility=View.VISIBLE
            etEmail.visibility=View.VISIBLE
            tvRegistration.visibility=View.VISIBLE

            groupAutoComplete.visibility=View.INVISIBLE
            etNameRegistration.visibility=View.INVISIBLE
            etSurNameRegistration.visibility=View.INVISIBLE
            etOtchestvoRegistration.visibility=View.INVISIBLE
            etEmailRegistration.visibility=View.INVISIBLE
            etPasswordRegistration.visibility=View.INVISIBLE
            btnRegistration.visibility=View.INVISIBLE
            tvGoBack.visibility=View.INVISIBLE
        }
        // Создаем список групп
        // Создаем список групп
        val groups = arrayOf("22ИСТ(б)СИЦ", "22ИБ(б)БАС-1", "23КБ(с)РЗПО-1")

        // Устанавливаем адаптер

        // Устанавливаем адаптер
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, groups
        )
        groupAutoComplete.setAdapter(adapter)

        // Открываем список при клике

        // Открываем список при клике
        groupAutoComplete.setOnClickListener { v -> groupAutoComplete.showDropDown() }

        // Обрабатываем выбор элемента

        // Обрабатываем выбор элемента
        groupAutoComplete.setOnItemClickListener { parent, view, position, id ->
            val selectedGroup: String = parent.getItemAtPosition(position).toString()
            // Делаем что-то с выбранным значением
            Toast.makeText(this, "Выбрана группа: $selectedGroup", Toast.LENGTH_SHORT).show()
        }
        //
        //Connection to Server TEST
        //
//        CoroutineScope(Dispatchers.IO).launch {
//            getRequest("http://10.0.2.2:3000/users")
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
}