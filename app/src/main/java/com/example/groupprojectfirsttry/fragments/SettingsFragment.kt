package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.ThemeManager
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.TrainingSession
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var llTrainerExtra: LinearLayout
    private lateinit var tvQuestionCountBadge: TextView
    private lateinit var btnStartTraining: MaterialButton
    private lateinit var switchTrainer: MaterialSwitch
    private var totalUnresolvedCount = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llTrainerExtra = view.findViewById(R.id.llTrainerExtra)
        tvQuestionCountBadge = view.findViewById(R.id.tvQuestionCountBadge)
        btnStartTraining = view.findViewById(R.id.btnStartTraining)
        switchTrainer = view.findViewById(R.id.switchTrainer)

        val isTrainerEnabled = ThemeManager.isTrainerEnabled(requireContext())
        switchTrainer.isChecked = isTrainerEnabled
        updateTrainerVisibility(isTrainerEnabled)

        switchTrainer.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setTrainerEnabled(requireContext(), isChecked)
            updateTrainerVisibility(isChecked)
            if (isChecked) {
                loadTrainingSessions()
            }
        }

        btnStartTraining.setOnClickListener {
            if (totalUnresolvedCount > 0) {
                (requireActivity() as? SecondActivityWithBottomNavMenu)?.replaceFragment(TrainingListFragment(), null)
            } else {
                Toast.makeText(context, "Нет доступных вопросов для тренировки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (switchTrainer.isChecked) {
            loadTrainingSessions()
        }
    }

    private fun updateTrainerVisibility(isEnabled: Boolean) {
        llTrainerExtra.visibility = if (isEnabled) View.VISIBLE else View.GONE
    }

    private fun loadTrainingSessions() {
        val userProvider = requireActivity() as? UserProvider ?: return
        val user = userProvider.getUser()
        val userId = user.id ?: return

        lifecycleScope.launch {
            try {
                // Запрашиваем все сессии пользователя
                val sessions = ApiClient.apiService.getTrainingSessions(userId)
                
                // 1. Фильтруем сессии именно этого пользователя
                val userSessions = sessions.filter { it.userId == userId }
                
                // 2. Считаем общее количество нерешенных вопросов во всех сессиях
                totalUnresolvedCount = userSessions.sumOf { session ->
                    session.questions?.count { it.status != "correct" } ?: 0
                }

                Log.d("SettingsFragment", "User: $userId | Total questions to fix: $totalUnresolvedCount")
                
                // 3. Обновляем UI
                tvQuestionCountBadge.text = "$totalUnresolvedCount вопросов"
                btnStartTraining.isEnabled = totalUnresolvedCount > 0
                
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error loading sessions", e)
                tvQuestionCountBadge.text = "Ошибка загрузки"
                btnStartTraining.isEnabled = false
            }
        }
    }
}
