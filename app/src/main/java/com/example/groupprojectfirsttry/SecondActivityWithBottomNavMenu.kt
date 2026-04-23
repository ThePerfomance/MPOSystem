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

    private var currentNavId: Int = -1

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second_with_bottom_nav_menu)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setupWindow()

        user = intent.getParcelableExtra("user")
            ?: throw IllegalArgumentException(getString(R.string.error_user_not_found))

        initViews()
        setupNavigation()

        val openSettings = intent.getBooleanExtra("open_settings", false)
        if (openSettings) {
            navigateTo(R.id.settingsFragment, useAnimation = false)
            bottomNav.selectedItemId = R.id.settingsFragment
        } else {
            navigateTo(R.id.homeFragment, useAnimation = false)
        }
    }

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_nav)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }
    }

    private fun initViews() {
        bottomNav   = findViewById(R.id.bottom_nav)
        tvUpper     = findViewById(R.id.textViewUpper)
        ivPencil    = findViewById(R.id.imageViewPencil)
        ivLupa      = findViewById(R.id.imageViewLupa)
        tvUserName  = findViewById(R.id.textViewUserName)

        tvUserName.text = "${user.lastname} ${user.firstname}"
    }

    private fun setupNavigation() {
        val isTeacher = user.role == "teacher"
        bottomNav.menu.clear()
        bottomNav.inflateMenu(R.menu.bottom_nav_menu)

        if (isTeacher) {
            bottomNav.menu.findItem(R.id.booksFragment)?.title = getString(R.string.title_journal)
        }

        bottomNav.setOnItemSelectedListener { item ->
            if (!canNavigate()) return@setOnItemSelectedListener false
            if (currentNavId == item.itemId) return@setOnItemSelectedListener false
            navigateTo(item.itemId)
            true
        }

        bottomNav.setOnItemReselectedListener { item ->
            if (!canNavigate()) return@setOnItemReselectedListener
            Log.d(TAG, "Nav item reselected: ${item.title}")
            navigateTo(item.itemId)
        }
    }

    fun canNavigate(): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is TestPassFragment) {
            // Если тест уже завершен (показан результат), разрешаем навигацию без диалога
            if (fragment.isFinished) return true

            fragment.showExitConfirmationDialog()
            return false
        }
        return true
    }

    fun navigateTo(itemId: Int, useAnimation: Boolean = true) {
        val isTeacher = user.role == "teacher"
        
        val enterAnim: Int
        val exitAnim: Int
        
        val menuItems = listOf(R.id.homeFragment, R.id.booksFragment, R.id.profileFragment, R.id.settingsFragment)
        val oldIndex = menuItems.indexOf(currentNavId)
        val newIndex = menuItems.indexOf(itemId)
        
        if (newIndex > oldIndex) {
            enterAnim = R.anim.slide_in_right
            exitAnim = R.anim.slide_out_left
        } else {
            enterAnim = R.anim.slide_in_left
            exitAnim = R.anim.slide_out_right
        }
        
        currentNavId = itemId
        clearBackStack()

        when (itemId) {
            R.id.homeFragment -> {
                replaceFragment(HomeFragment(), enterAnim, exitAnim, useAnimation)
                tvUpper.text = getString(R.string.title_home)
            }
            R.id.booksFragment -> {
                val fragment = if (isTeacher) {
                    JournalFragment()
                } else if (BuildConfig.FLAVOR == "impuls") {
                    OnboardingFragment()
                } else {
                    BooksFragment()
                }
                replaceFragment(fragment, enterAnim, exitAnim, useAnimation)
                tvUpper.text = if (isTeacher) getString(R.string.title_journal) else getString(R.string.title_books)
            }
            R.id.profileFragment -> {
                replaceFragment(
                    if (isTeacher) ProfileFragment() else ProfileAndTestResultsFragment(),
                    enterAnim, exitAnim, useAnimation
                )
                tvUpper.text = getString(R.string.title_profile)
            }
            R.id.settingsFragment -> {
                replaceFragment(SettingsFragment(), enterAnim, exitAnim, useAnimation)
                tvUpper.text = getString(R.string.title_settings)
            }
        }
    }

    private fun replaceFragment(
        fragment: Fragment, 
        enterAnim: Int = R.anim.slide_in_right, 
        exitAnim: Int = R.anim.slide_out_left,
        useAnimation: Boolean = true
    ) {
        Log.d(TAG, "Replace → ${fragment::class.java.simpleName}")
        val transaction = supportFragmentManager.beginTransaction()
        if (useAnimation) {
            transaction.setCustomAnimations(enterAnim, exitAnim)
        }
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    fun replaceFragment(fragment: Fragment, args: Bundle? = null) {
        Log.d(TAG, "Replace (animated with backstack) → ${fragment::class.java.simpleName}")
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
