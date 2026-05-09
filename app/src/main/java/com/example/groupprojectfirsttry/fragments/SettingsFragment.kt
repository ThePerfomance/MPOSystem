package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.ThemeManager
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.TrainingSession
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var llTrainerExtra: LinearLayout
    private lateinit var tvQuestionCountBadge: TextView
    private lateinit var btnStartTraining: MaterialButton
    private lateinit var switchTrainer: MaterialSwitch
    private lateinit var shimmerSettingsBadge: ShimmerFrameLayout
    private lateinit var swipeRefreshSettings: SwipeRefreshLayout
    private var totalUnresolvedCount = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llTrainerExtra = view.findViewById(R.id.llTrainerExtra)
        tvQuestionCountBadge = view.findViewById(R.id.tvQuestionCountBadge)
        btnStartTraining = view.findViewById(R.id.btnStartTraining)
        switchTrainer = view.findViewById(R.id.switchTrainer)
        shimmerSettingsBadge = view.findViewById(R.id.shimmerSettingsBadge)
        swipeRefreshSettings = view.findViewById(R.id.swipeRefreshSettings)

        val isTrainerEnabled = ThemeManager.isTrainerEnabled(requireContext())
        switchTrainer.isChecked = isTrainerEnabled
        updateTrainerVisibility(isTrainerEnabled)

        switchTrainer.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setTrainerEnabled(requireContext(), isChecked)
            updateTrainerVisibility(isChecked)
            if (isChecked) {
                loadTrainingSessions(isRefresh = false)
            }
        }

        btnStartTraining.setOnClickListener {
            if (totalUnresolvedCount > 0) {
                (requireActivity() as? SecondActivityWithBottomNavMenu)
                    ?.replaceFragment(TrainingListFragment(), null)
            }
        }

        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        swipeRefreshSettings.setColorSchemeResources(R.color.AccentColor)
        swipeRefreshSettings.setOnRefreshListener {
            if (switchTrainer.isChecked) {
                loadTrainingSessions(isRefresh = true)
            } else {
                swipeRefreshSettings.isRefreshing = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (switchTrainer.isChecked) {
            loadTrainingSessions(isRefresh = false)
        }
    }

    private fun updateTrainerVisibility(isEnabled: Boolean) {
        if (!isEnabled) {
            llTrainerExtra.visibility = View.GONE
            return
        }
        llTrainerExtra.visibility = View.VISIBLE
    }

    private fun loadTrainingSessions(isRefresh: Boolean = false) {
        val userProvider = requireActivity() as? UserProvider ?: return
        val user = userProvider.getUser()
        val userId = user.id ?: return

        if (!isRefresh) {
            shimmerSettingsBadge.startShimmer()
            tvQuestionCountBadge.text = "Загрузка..."
        }

        lifecycleScope.launch {
            try {
                val sessions = ApiClient.apiService.getTrainingSessions(userId)
                
                totalUnresolvedCount = sessions
                    .filter { it.status != "completed" }
                    .sumOf { session ->
                        session.questions?.count { 
                            it.status == "pending" || it.status == "wrong" 
                        } ?: 0
                    }

                Log.d("SettingsFragment", "User: $userId | Total unresolved: $totalUnresolvedCount")
                
                if (totalUnresolvedCount > 0) {
                    tvQuestionCountBadge.text = "$totalUnresolvedCount вопросов"
                    btnStartTraining.visibility = View.VISIBLE
                    btnStartTraining.isEnabled = true
                } else {
                    tvQuestionCountBadge.text = "Вопросы отсутствуют"
                    btnStartTraining.visibility = View.GONE
                }
                
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error loading training status", e)
                tvQuestionCountBadge.text = "Ошибка загрузки"
                btnStartTraining.visibility = View.GONE
            } finally {
                if (isAdded) {
                    shimmerSettingsBadge.stopShimmer()
                    shimmerSettingsBadge.setShimmer(null)
                    swipeRefreshSettings.isRefreshing = false
                }
            }
        }
    }
}
