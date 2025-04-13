package com.example.groupprojectfirsttry

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.api.AddUserToGroupRequest
import com.example.groupprojectfirsttry.api.ApiClient.apiService
import com.example.groupprojectfirsttry.api.Group
import com.example.groupprojectfirsttry.fragments.BooksFragment
import com.example.groupprojectfirsttry.fragments.HomeFragment
import com.example.groupprojectfirsttry.fragments.JournalFragment
import com.example.groupprojectfirsttry.fragments.ProfileAndTestResultsFragment
import com.example.groupprojectfirsttry.fragments.ProfileFragment
import com.example.groupprojectfirsttry.fragments.SettingsFragment
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.UUID

class SecondActivityWithBottomNavMenu : AppCompatActivity(), UserProvider {
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvUpper: TextView
    private lateinit var ivPencil: ImageView
    private lateinit var ivLupa: ImageView
    private lateinit var user: User
    private lateinit var tvUserName:TextView

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включение edge-to-edge режима
        enableEdgeToEdge()
        setContentView(R.layout.activity_second_with_bottom_nav_menu)

        // Настройка отступов для системных панелей
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_nav)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }
        // Фиксация ориентации экрана
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Скрытие системных панелей
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Получение данных пользователя из Intent
        user = intent.getParcelableExtra("user") ?: run {
            throw IllegalArgumentException("User not found")
        }
        Log.d("SecondActivity", "User data retrieved: Name=${user.firstname}, Email=${user.email}")

        // Инициализация UI элементов
        bottomNav = findViewById(R.id.bottom_nav)
        tvUpper = findViewById(R.id.textViewUpper)
        ivPencil = findViewById(R.id.imageViewPencil)
        ivLupa = findViewById(R.id.imageViewLupa)
        tvUserName=findViewById(R.id.textViewUserName)

        tvUserName.text=getUser().lastname+" " + getUser().firstname
        // Установка начального фрагмента
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
        Log.d("SecondActivity", "HomeFragment set as initial fragment")

        // Обработка нажатий на нижнее меню
        when(user.role)
        {
            "student"->
            {
                bottomNav.menu.clear()
                bottomNav.inflateMenu(R.menu.bottom_nav_menu)
                bottomNav.setOnItemSelectedListener { item ->
                    Log.d("SecondActivity", "Bottom navigation item selected: ${item.title}")
                    when (item.itemId) {
                        R.id.homeFragment -> {
                            clearBackStack()
                            replaceFragment(HomeFragment())
                            tvUpper.text = "Главная"
                            Log.d("SecondActivity", "Switched to HomeFragment")
                            true
                        }
                        R.id.booksFragment -> {
                            clearBackStack()
                            replaceFragment(BooksFragment())
                            tvUpper.text = "Электронный учебник"
                            Log.d("SecondActivity", "Switched to BooksFragment")
                            true
                        }
                        R.id.profileFragment -> {
                            clearBackStack()
                            replaceFragment(ProfileAndTestResultsFragment())
                            tvUpper.text = "Профиль"
                            Log.d("SecondActivity", "Switched to ProfileAndTestResultsFragment")
                            true
                        }
                        R.id.settingsFragment -> {
                            clearBackStack()
                            replaceFragment(SettingsFragment())
                            tvUpper.text = "Настройки"
                            Log.d("SecondActivity", "Switched to SettingsFragment")
                            true
                        }
                        else -> false
                    }
                }
            }
            "teacher"->{
                bottomNav.menu.clear()
                bottomNav.inflateMenu(R.menu.bottom_nav_menu_teacher)
                bottomNav.setOnItemSelectedListener { item ->
                    Log.d("SecondActivity", "Bottom navigation item selected: ${item.title}")
                    when (item.itemId) {
                        R.id.homeFragment -> {
                            clearBackStack()
                            replaceFragment(HomeFragment())
                            tvUpper.text = "Главная"
                            Log.d("SecondActivity", "Switched to HomeFragment")
                            true
                        }
                        R.id.booksFragment -> {
                            clearBackStack()
                            replaceFragment(JournalFragment())
                            tvUpper.text = "Журнал"
                            Log.d("SecondActivity", "Switched to JournalFragment")
                            true
                        }
                        R.id.profileFragment -> {
                            clearBackStack()
                            replaceFragment(ProfileFragment())
                            tvUpper.text = "Профиль"
                            Log.d("SecondActivity", "Switched to ProfileFragment")
                            true
                        }
                        R.id.settingsFragment -> {
                            clearBackStack()
                            replaceFragment(SettingsFragment())
                            tvUpper.text = "Настройки"
                            Log.d("SecondActivity", "Switched to SettingsFragment")
                            true
                        }
                        else -> false
                    }
                }
            }
            else ->{
                bottomNav.menu.clear()
                bottomNav.inflateMenu(R.menu.bottom_nav_menu)
                bottomNav.setOnItemSelectedListener { item ->
                    Log.d("SecondActivity", "Bottom navigation item selected: ${item.title}")
                    when (item.itemId) {
                        R.id.homeFragment -> {
                            clearBackStack()
                            replaceFragment(HomeFragment())
                            tvUpper.text = "Главная"
                            Log.d("SecondActivity", "Switched to HomeFragment")
                            true
                        }
                        R.id.booksFragment -> {
                            clearBackStack()
                            replaceFragment(BooksFragment())
                            tvUpper.text = "Электронный учебник"
                            Log.d("SecondActivity", "Switched to BooksFragment")
                            true
                        }
                        R.id.profileFragment -> {
                            clearBackStack()
                            replaceFragment(ProfileFragment())
                            tvUpper.text = "Профиль"
                            Log.d("SecondActivity", "Switched to ProfileFragment")
                            true
                        }
                        R.id.settingsFragment -> {
                            clearBackStack()
                            replaceFragment(SettingsFragment())
                            tvUpper.text = "Настройки"
                            Log.d("SecondActivity", "Switched to SettingsFragment")
                            true
                        }
                        else -> false
                    }
                }
            }
        }

    }

    private fun replaceFragment(fragment: Fragment) {
        Log.d("SecondActivity", "Replacing fragment: ${fragment::class.java.simpleName}")
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    fun replaceFragment(fragment: Fragment, args: Bundle? = null) {
        Log.d("SecondActivity", "Replacing fragment with arguments: ${fragment::class.java.simpleName}")
        fragment.arguments = args
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, // Анимация для входящего фрагмента (слева направо)
                R.anim.slide_out_left, // Анимация для исходящего фрагмента (справа налево)
                R.anim.slide_in_left,  // Анимация для возврата (справа налево)
                R.anim.slide_out_right // Анимация для закрытия (слева направо)
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    private fun clearBackStack() {
        Log.d("SecondActivity", "Clearing back stack")
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }
    override fun getUser(): User {
        Log.d("SecondActivity", "Providing user data: Name=${user.firstname}, Email=${user.email}")
        return user
    }

            override suspend fun getUserGroups(): List<Group>? {
                Log.d("SecondActivity", "Fetching user groups for user: ${user.firstname}")
                return withContext(Dispatchers.IO) {
                    try {
                        val userId = user.id
                        if (userId == null) {
                            Log.e("SecondActivity", "User ID is null, cannot fetch groups")
                            return@withContext null
                        }
                        Log.d("SecondActivity", "Fetching groups for user ID: $userId")
                        val groups = apiService.getUserGroups(userId)
                        Log.d("SecondActivity", "Fetched groups: ${groups.size} groups")
                        groups
                    } catch (e: HttpException) {
                        Log.e("SecondActivity", "HTTP Exception while fetching groups: ${e.code()} - ${e.message()}", e)
                        null
                    } catch (e: Exception) {
                        Log.e("SecondActivity", "Error fetching user groups: ${e.message}", e)
                        null
                    }
                }
            }
}