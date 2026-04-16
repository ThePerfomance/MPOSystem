package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.ThemeManager
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var llTrainerExtra: LinearLayout
    private lateinit var tvQuestionCountBadge: TextView
    private lateinit var btnStartTraining: MaterialButton
    private lateinit var switchTrainer: MaterialSwitch

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llTrainerExtra = view.findViewById(R.id.llTrainerExtra)
        tvQuestionCountBadge = view.findViewById(R.id.tvQuestionCountBadge)
        btnStartTraining = view.findViewById(R.id.btnStartTraining)
        switchTrainer = view.findViewById(R.id.switchTrainer)

        val isTrainerEnabled = ThemeManager.isTrainerEnabled(requireContext())
        switchTrainer.isChecked = isTrainerEnabled
        updateTrainerVisibility(isTrainerEnabled)

        if (isTrainerEnabled) {
            loadTrainingSessions()
        }

        switchTrainer.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setTrainerEnabled(requireContext(), isChecked)
            updateTrainerVisibility(isChecked)
            if (isChecked) {
                loadTrainingSessions()
            }
        }

        btnStartTraining.setOnClickListener {
            // Здесь будет переход к самому тесту тренажёра
            Log.d("SettingsFragment", "Start training clicked")
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
                val sessions = ApiClient.apiService.getTrainingSessions(userId)
                val uncompletedSessions = sessions.filter { !it.isCompleted }
                
                // Считаем общее количество нерешенных вопросов во всех активных сессиях
                val totalQuestions = uncompletedSessions.sumOf { session ->
                    session.questions?.count { !it.isResolved } ?: 0
                }

                tvQuestionCountBadge.text = "$totalQuestions вопросов"
                btnStartTraining.isEnabled = totalQuestions > 0
                
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error loading training sessions", e)
                tvQuestionCountBadge.text = "Ошибка загрузки"
                btnStartTraining.isEnabled = false
            }
        }
    }
}