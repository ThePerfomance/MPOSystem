package com.example.groupprojectfirsttry

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
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
import com.google.android.material.bottomnavigation.BottomNavigationView

class SecondActivityWithBottomNavMenu : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvUpper:TextView
    private lateinit var ivPencil:ImageView
    private lateinit var ivLupa:ImageView

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
                    tvUpper.text = "Теоретический\nматериал"
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
    private fun clearBackStack() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }
}