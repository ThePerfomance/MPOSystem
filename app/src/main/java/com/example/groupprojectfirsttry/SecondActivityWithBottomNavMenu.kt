package com.example.groupprojectfirsttry

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.groupprojectfirsttry.api.ApiClient.apiService
import com.example.groupprojectfirsttry.api.Group
import com.example.groupprojectfirsttry.fragments.BooksFragment
import com.example.groupprojectfirsttry.fragments.HomeFragment
import com.example.groupprojectfirsttry.fragments.ProfileFragment
import com.example.groupprojectfirsttry.fragments.SettingsFragment
import com.example.groupprojectfirsttry.simpleClasses.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SecondActivityWithBottomNavMenu : AppCompatActivity(), UserProvider {
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvUpper:TextView
    private lateinit var ivPencil:ImageView
    private lateinit var ivLupa:ImageView
    private lateinit var user: User

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second_with_bottom_nav_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_nav)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars.
        // Configure the behavior of the hidden system bars.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        // Получение User из Intent
        user = intent.getParcelableExtra("user") ?: throw IllegalArgumentException("User not found")
        // Пример использования данных пользователя
        Log.d("USER", "Имя: ${user.firstname}, Email: ${user.email}")
        bottomNav = findViewById(R.id.bottom_nav)
        tvUpper=findViewById(R.id.textViewUpper)
        ivPencil=findViewById(R.id.imageViewPencil)
        ivLupa=findViewById(R.id.imageViewLupa)
        // Установка начального фрагмента
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        // Обработка нажатий
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    clearBackStack()
                    replaceFragment(HomeFragment())
                    tvUpper.text = "Главная"
                    ivPencil.visibility= View.INVISIBLE
                    ivLupa.visibility= View.VISIBLE
                    true
                }
                R.id.booksFragment -> {
                    clearBackStack()
                    replaceFragment(BooksFragment())
                    tvUpper.text = "Учебник"
                    ivPencil.visibility= View.INVISIBLE
                    ivLupa.visibility= View.INVISIBLE
                    true
                }
                R.id.profileFragment -> {
                    clearBackStack()
                    replaceFragment(ProfileFragment())
                    tvUpper.text = "Профиль"
                    ivPencil.visibility= View.VISIBLE
                    ivLupa.visibility= View.INVISIBLE
                    true
                }
                R.id.settingsFragment -> {
                    clearBackStack()
                    replaceFragment(SettingsFragment())
                    tvUpper.text = "Настройки"
                    ivPencil.visibility= View.INVISIBLE
                    ivLupa.visibility= View.INVISIBLE
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    fun replaceFragment(fragment: Fragment, args: Bundle? = null) {
        fragment.arguments = args
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    private fun clearBackStack() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }
    override fun getUser(): User {
        return user
    }
    override suspend fun getUserGroups(): List<Group>? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GetUserGroups", "Starting to fetch user groups for user: $user")
                val userId = user.id
                if (userId == null) {
                    Log.e("GetUserGroups", "User ID is null")
                    return@withContext null
                }
                val groups = apiService.getUserGroups(userId)
                Log.d("GetUserGroups", "Fetched groups: $groups")
                groups
            } catch (e: HttpException) {
                Log.e("GetUserGroups", "HTTP Exception: ${e.code()} - ${e.message()}", e)
                null
            } catch (e: Exception) {
                Log.e("GetUserGroups", "Error fetching user groups: ${e.message}", e)
                null
            }
        }
    }
}