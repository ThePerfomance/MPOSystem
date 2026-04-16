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
                // Переходим к списку всех работ над ошибками
                (requireActivity() as? SecondActivityWithBottomNavMenu)
                    ?.replaceFragment(TrainingListFragment(), null)
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
                // Получаем все сессии (API уже фильтрует по user_id)
                val sessions = ApiClient.apiService.getTrainingSessions(userId)
                
                // Согласно документации:
                // Считаем вопросы со статусом 'pending' или 'wrong' во всех сессиях,
                // где статус самой сессии не равен 'completed'
                totalUnresolvedCount = sessions
                    .filter { it.status != "completed" }
                    .sumOf { session ->
                        session.questions?.count { 
                            it.status == "pending" || it.status == "wrong" 
                        } ?: 0
                    }

                Log.d("SettingsFragment", "User: $userId | Total unresolved: $totalUnresolvedCount")
                
                // Обновляем UI
                if (totalUnresolvedCount > 0) {
                    tvQuestionCountBadge.text = "$totalUnresolvedCount вопросов"
                } else {
                    tvQuestionCountBadge.text = "Ошибок нет"
                }

                btnStartTraining.isEnabled = totalUnresolvedCount > 0
                
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error loading training status", e)
                tvQuestionCountBadge.text = "Ошибка загрузки"
                btnStartTraining.isEnabled = false
            }
        }
    }
}
