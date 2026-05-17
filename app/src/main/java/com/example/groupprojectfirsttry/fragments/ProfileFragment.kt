package com.example.groupprojectfirsttry.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.groupprojectfirsttry.BuildConfig
import com.example.groupprojectfirsttry.MainActivity
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TokenManager
import com.example.groupprojectfirsttry.simpleClasses.User
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    private lateinit var etSurname: EditText
    private lateinit var etName: EditText
    private lateinit var etPatronymic: EditText
    private lateinit var etGroup: EditText
    private lateinit var tvGroupLabel: TextView
    private lateinit var tvCenterTitle: TextView
    private lateinit var imgExit: ImageView
    private lateinit var viewDividerGroup: View
    private lateinit var btnBackProfile: View
    
    // UI элементы для рейтинга
    private lateinit var llRatingContainer: View
    private lateinit var tvRatingValue: TextView
    private lateinit var pbRating: ProgressBar
    private lateinit var tvClusterLabel: TextView
    
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var profileContent: View
    private lateinit var swipeRefreshProfile: SwipeRefreshLayout
    private lateinit var tokenManager: TokenManager
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tokenManager = TokenManager(requireContext())
        shimmerContainer = view.findViewById(R.id.shimmer_view_container)
        profileContent = view.findViewById(R.id.profile_content)
        swipeRefreshProfile = view.findViewById(R.id.swipeRefreshProfile)

        etSurname = view.findViewById(R.id.editTextText)
        etName = view.findViewById(R.id.editTextText2)
        etPatronymic = view.findViewById(R.id.editTextText3)
        etGroup = view.findViewById(R.id.editTextText4)
        tvGroupLabel = view.findViewById(R.id.textViewGroup)
        imgExit = view.findViewById(R.id.imageViewExit)
        viewDividerGroup = view.findViewById(R.id.viewDividerGroup)
        btnBackProfile = view.findViewById(R.id.btnBackProfile)

        // Инициализация рейтинга
        llRatingContainer = view.findViewById(R.id.llRatingContainer)
        tvRatingValue = view.findViewById(R.id.tvRatingValue)
        pbRating = view.findViewById(R.id.pbRating)
        tvClusterLabel = view.findViewById(R.id.tvClusterLabel)

        tvCenterTitle = requireActivity().findViewById(R.id.textViewUpper)

        btnBackProfile.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupSwipeRefresh()

        // 1. Сначала показываем то, что уже есть в Activity
        val activity = requireActivity() as SecondActivityWithBottomNavMenu
        updateUI(activity.getUser())

        // 2. Загружаем свежие данные с сервера
        refreshUserData(isRefresh = false)

        return view
    }

    private fun setupSwipeRefresh() {
        swipeRefreshProfile.setColorSchemeResources(R.color.AccentColor)
        swipeRefreshProfile.setOnRefreshListener {
            refreshUserData(isRefresh = true)
        }
    }

    private fun refreshUserData(isRefresh: Boolean = false) {
        val activity = requireActivity() as? SecondActivityWithBottomNavMenu ?: return
        val email = tokenManager.getUserEmail()

        if (email == null) {
            swipeRefreshProfile.isRefreshing = false
            return
        }

        // Всегда показываем скелетон при обновлении, как просил пользователь
        startLoading()
        
        lifecycleScope.launch {
            try {
                val updatedUser = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getUserByEmail(email)
                }
                
                activity.updateCurrentUser(updatedUser)
                updateUI(updatedUser)

                if (updatedUser.role == "student") {
                    val groups = activity.getUserGroups()
                    if (!groups.isNullOrEmpty()) {
                        val groupNames = groups.joinToString(", ") { it.name }
                        etGroup.setText(groupNames)
                    } else {
                        etGroup.setText("Группа не назначена")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error in refreshUserData", e)
            } finally {
                if (isAdded) {
                    stopLoading()
                    swipeRefreshProfile.isRefreshing = false
                }
            }
        }
    }

    private fun updateUI(user: User) {
        etSurname.setText(user.lastname)
        etName.setText(user.firstname)
        etPatronymic.setText(user.patronymic)

        if (user.role == "student") {
            tvGroupLabel.visibility = View.VISIBLE
            etGroup.visibility = View.VISIBLE
            viewDividerGroup.visibility = View.VISIBLE
            imgExit.visibility = View.GONE

            if (BuildConfig.SHOW_DIFFICULTY_AND_RATING) {
                llRatingContainer.visibility = View.VISIBLE
                val rating = user.rating ?: 0.0
                tvRatingValue.text = String.format("%.1f", rating)
                pbRating.progress = rating.toInt()
                tvClusterLabel.text = "Кластер: ${user.clusterLabel ?: "Не определен"}"
            } else {
                llRatingContainer.visibility = View.GONE
            }
        } else {
            tvGroupLabel.visibility = View.GONE
            etGroup.visibility = View.GONE
            viewDividerGroup.visibility = View.GONE
            llRatingContainer.visibility = View.GONE
            imgExit.visibility = View.VISIBLE
            imgExit.setOnClickListener { showExitDialog() }
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Да") { _, _ ->
                tokenManager.clear()
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun startLoading() {
        shimmerContainer.visibility = View.VISIBLE
        shimmerContainer.startShimmer()
        profileContent.visibility = View.GONE
    }

    private fun stopLoading() {
        if (!isAdded) return
        shimmerContainer.stopShimmer()
        shimmerContainer.visibility = View.GONE
        profileContent.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        tvCenterTitle.text = "Данные профиля"
    }
}
