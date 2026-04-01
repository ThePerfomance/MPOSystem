package com.example.groupprojectfirsttry

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.groupprojectfirsttry.api.ApiClient.apiService
import com.example.groupprojectfirsttry.api.Group
import com.example.groupprojectfirsttry.fragments.*
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SecondActivityWithBottomNavMenu : AppCompatActivity(), UserProvider {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvUpper: TextView
    private lateinit var ivPencil: ImageView
    private lateinit var ivLupa: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var user: User

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second_with_bottom_nav_menu)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setupWindow()

        user = intent.getParcelableExtra("user")
            ?: throw IllegalArgumentException("User not found")
        Log.d(TAG, "User: ${user.firstname}, ${user.email}")

        initViews()
        setupNavigation()

        replaceFragment(HomeFragment())
    }

    //Window & Insets

    private fun setupWindow() {
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        setupInsets()
    }

    private fun setupInsets() {
        // Отступы для шапки
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<ConstraintLayout>(R.id.constraintLayoutUpHead)
        ) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val topInset = maxOf(systemBars.top, cutout.top)

            val defaultHeightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 121f, resources.displayMetrics
            ).toInt()

            (view.layoutParams as? ConstraintLayout.LayoutParams)?.let {
                it.height = defaultHeightPx + topInset
                view.layoutParams = it
            }
            view.setPadding(systemBars.left, topInset, systemBars.right, systemBars.bottom)
            insets
        }

        // Отступы для нижнего меню
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_nav)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }
    }

    //Init

    private fun initViews() {
        bottomNav   = findViewById(R.id.bottom_nav)
        tvUpper     = findViewById(R.id.textViewUpper)
        ivPencil    = findViewById(R.id.imageViewPencil)
        ivLupa      = findViewById(R.id.imageViewLupa)
        tvUserName  = findViewById(R.id.textViewUserName)

        tvUserName.text = "${user.lastname} ${user.firstname}"
    }

    //Navigation

    private fun setupNavigation() {
        val isTeacher = user.role == "teacher"

        // Выбираем нужное меню
        bottomNav.menu.clear()
        bottomNav.inflateMenu(
            if (isTeacher) R.menu.bottom_nav_menu_teacher
            else R.menu.bottom_nav_menu
        )

        bottomNav.setOnItemSelectedListener { item ->
            Log.d(TAG, "Nav item: ${item.title}")
            clearBackStack()

            when (item.itemId) {
                R.id.homeFragment -> {
                    replaceFragment(HomeFragment())
                    tvUpper.text = "Главная"
                    true
                }
                R.id.booksFragment -> {
                    // Учитель видит журнал, остальные — учебник
                    replaceFragment(
                        if (isTeacher) JournalFragment() else BooksFragment()
                    )
                    tvUpper.text = if (isTeacher) "Журнал" else "Электронный учебник"
                    true
                }
                R.id.profileFragment -> {
                    // Учитель видит обычный профиль, студент — профиль с результатами
                    replaceFragment(
                        if (isTeacher) ProfileFragment() else ProfileAndTestResultsFragment()
                    )
                    tvUpper.text = "Профиль"
                    true
                }
                R.id.settingsFragment -> {
                    replaceFragment(SettingsFragment())
                    tvUpper.text = "Настройки"
                    true
                }
                else -> false
            }
        }
    }

    //Fragment helpers

    private fun replaceFragment(fragment: Fragment) {
        Log.d(TAG, "Replace → ${fragment::class.java.simpleName}")
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun replaceFragment(fragment: Fragment, args: Bundle? = null) {
        Log.d(TAG, "Replace (animated) → ${fragment::class.java.simpleName}")
        fragment.arguments = args
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun clearBackStack() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            Log.d(TAG, "Back stack cleared")
        }
    }

    // UserProvider

    override fun getUser(): User = user

    override suspend fun getUserGroups(): List<Group>? =
        withContext(Dispatchers.IO) {
            try {
                val userId = user.id ?: run {
                    Log.e(TAG, "User ID is null")
                    return@withContext null
                }
                apiService.getUserGroups(userId).also {
                    Log.d(TAG, "Fetched ${it.size} groups")
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP ${e.code()}: ${e.message()}", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching groups: ${e.message}", e)
                null
            }
        }

    companion object {
        private const val TAG = "SecondActivity"
    }
}