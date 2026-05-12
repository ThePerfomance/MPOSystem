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
import com.example.groupprojectfirsttry.api.AdaptiveTrainingRequest
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.TrainingSession
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch
import java.util.UUID

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var llTrainerExtra: LinearLayout
    private lateinit var llAdaptiveTrainerSwitch: LinearLayout
    private lateinit var tvQuestionCountBadge: TextView
    private lateinit var btnStartTraining: MaterialButton
    private lateinit var switchTrainer: MaterialSwitch
    private lateinit var switchAdaptiveTrainer: MaterialSwitch
    private lateinit var shimmerSettingsBadge: ShimmerFrameLayout
    private lateinit var swipeRefreshSettings: SwipeRefreshLayout
    private var totalUnresolvedCount = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llTrainerExtra = view.findViewById(R.id.llTrainerExtra)
        llAdaptiveTrainerSwitch = view.findViewById(R.id.llAdaptiveTrainerSwitch)
        tvQuestionCountBadge = view.findViewById(R.id.tvQuestionCountBadge)
        btnStartTraining = view.findViewById(R.id.btnStartTraining)
        switchTrainer = view.findViewById(R.id.switchTrainer)
        switchAdaptiveTrainer = view.findViewById(R.id.switchAdaptiveTrainer)
        shimmerSettingsBadge = view.findViewById(R.id.shimmerSettingsBadge)
        swipeRefreshSettings = view.findViewById(R.id.swipeRefreshSettings)

        val isTrainerEnabled = ThemeManager.isTrainerEnabled(requireContext())
        val isAdaptiveEnabled = ThemeManager.isAdaptiveTrainerEnabled(requireContext())
        
        switchTrainer.isChecked = isTrainerEnabled
        switchAdaptiveTrainer.isChecked = isAdaptiveEnabled
        
        updateTrainerVisibility(isTrainerEnabled)

        switchTrainer.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setTrainerEnabled(requireContext(), isChecked)
            updateTrainerVisibility(isChecked)
            if (isChecked) {
                loadTrainingSessions(isRefresh = false)
            }
        }
        
        switchAdaptiveTrainer.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setAdaptiveTrainerEnabled(requireContext(), isChecked)
            updateButtonText(isChecked)
        }

        btnStartTraining.setOnClickListener {
            if (ThemeManager.isAdaptiveTrainerEnabled(requireContext())) {
                startAdaptiveTraining()
            } else if (totalUnresolvedCount > 0) {
                (requireActivity() as? SecondActivityWithBottomNavMenu)
                    ?.replaceFragment(TrainingListFragment(), null)
            }
        }

        setupSwipeRefresh()
        updateButtonText(isAdaptiveEnabled)
    }
    
    private fun updateButtonText(isAdaptive: Boolean) {
        btnStartTraining.text = if (isAdaptive) "Запустить адаптивный тренажёр" else "Начать тренировку"
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
            llAdaptiveTrainerSwitch.visibility = View.GONE
            return
        }
        llTrainerExtra.visibility = View.VISIBLE
        llAdaptiveTrainerSwitch.visibility = View.VISIBLE
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
                
                // Исправлено: считаем все вопросы, которые не "correct"
                totalUnresolvedCount = sessions.sumOf { session ->
                        session.questions?.count { it.status != "correct" } ?: 0
                    }

                if (totalUnresolvedCount > 0) {
                    tvQuestionCountBadge.text = "$totalUnresolvedCount вопросов"
                    btnStartTraining.visibility = View.VISIBLE
                    btnStartTraining.isEnabled = true
                } else {
                    tvQuestionCountBadge.text = "Вопросы отсутствуют"
                    if (ThemeManager.isAdaptiveTrainerEnabled(requireContext())) {
                        btnStartTraining.visibility = View.VISIBLE
                        btnStartTraining.isEnabled = true
                    } else {
                        btnStartTraining.visibility = View.GONE
                    }
                }

            } catch (e: Exception) {
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
    
    private fun startAdaptiveTraining() {
        btnStartTraining.isEnabled = false
        btnStartTraining.text = "Подбор вопросов..."
        
        lifecycleScope.launch {
            try {
                // Используем объект запроса AdaptiveTrainingRequest вместо Map
                val request = AdaptiveTrainingRequest(
                    lessonId = null,
                    onlyPassed = true,
                    excludeCorrect = true
                )
                
                val response = ApiClient.apiService.createAdaptiveTrainingSession(request)
                if (response.isSuccessful && isAdded) {
                    val session = response.body()?.session
                    if (session != null && !session.questions.isNullOrEmpty()) {
                        val bundle = Bundle().apply {
                            putParcelable("session", session)
                            putBoolean("is_adaptive", true)
                        }
                        (requireActivity() as? SecondActivityWithBottomNavMenu)
                            ?.replaceFragment(TrainingFragment(), bundle)
                    } else {
                        Toast.makeText(requireContext(), "Вопросы не найдены", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Ошибка сессии", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show()
            } finally {
                if (isAdded) {
                    btnStartTraining.isEnabled = true
                    updateButtonText(true)
                }
            }
        }
    }
}
