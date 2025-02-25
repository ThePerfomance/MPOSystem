package com.example.groupprojectfirsttry

import android.R.attr.value
import android.app.VoiceInteractor
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var btnSignInApp:Button
    private lateinit var tvPrepodClick:TextView
    private lateinit var tvStudentClick:TextView
    private lateinit var etGroup:EditText

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
        tvPrepodClick=findViewById(R.id.textViewPrepod)
        tvStudentClick=findViewById(R.id.textViewStudent)
        etGroup=findViewById(R.id.editTextTextGroup)
        //
        ///////////////////////////////////////////////////
        //
        btnSignInApp.setOnClickListener {
            val intent = Intent(this, SecondActivityWithBottomNavMenu::class.java)
            intent.putExtra("key", value)
            startActivity(intent)
        }
        tvPrepodClick.setOnClickListener{
            if (tvPrepodClick.visibility == View.VISIBLE)
            {
                tvPrepodClick.visibility=View.INVISIBLE
                etGroup.visibility=View.INVISIBLE
                tvStudentClick.visibility=View.VISIBLE
            }
        }
        tvStudentClick.setOnClickListener{
            if (tvStudentClick.visibility == View.VISIBLE)
            {
                tvStudentClick.visibility=View.INVISIBLE
                etGroup.visibility=View.VISIBLE
                tvPrepodClick.visibility=View.VISIBLE
            }
        }
        //
        //Connection to Server TEST
        //
        /*CoroutineScope(Dispatchers.IO).launch {
            getRequest("http://10.0.2.2:3000/users")

        }*/

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