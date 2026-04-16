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
    private var activeSession: TrainingSession? = null

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
            Log.d("SettingsFragment", "btnStartTraining clicked. activeSession: ${activeSession?.id}")
            activeSession?.let { session ->
                val bundle = Bundle().apply {
                    putParcelable("session", session)
                }
                (requireActivity() as? SecondActivityWithBottomNavMenu)?.replaceFragment(TrainingFragment(), bundle)
            } ?: run {
                Log.w("SettingsFragment", "Attempted to start training but activeSession is null")
                Toast.makeText(context, "Нет активных сессий для тренировки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTrainerVisibility(isEnabled: Boolean) {
        llTrainerExtra.visibility = if (isEnabled) View.VISIBLE else View.GONE
    }

    private fun loadTrainingSessions() {
        val userProvider = requireActivity() as? UserProvider ?: return
        val user = userProvider.getUser()
        val userId = user.id ?: run {
            Log.e("SettingsFragment", "UserId is null, cannot load sessions")
            return
        }

        Log.d("SettingsFragment", "Loading training sessions for userId: $userId")

        lifecycleScope.launch {
            try {
                val sessions = ApiClient.apiService.getTrainingSessions(userId)
                Log.d("SettingsFragment", "Received ${sessions.size} sessions from API")
                
                val uncompletedSessions = sessions.filter { !it.isCompleted }
                Log.d("SettingsFragment", "Found ${uncompletedSessions.size} uncompleted sessions")
                
                // Берем первую незавершенную сессию как активную
                activeSession = uncompletedSessions.firstOrNull()
                Log.d("SettingsFragment", "Active session set to: ${activeSession?.id}")

                // Считаем общее количество нерешенных вопросов во всех активных сессиях
                val totalQuestions = uncompletedSessions.sumOf { session ->
                    val count = session.questions?.count { !it.isResolved } ?: 0
                    Log.d("SettingsFragment", "Session ${session.id}: ${count} unresolved questions")
                    count
                }

                Log.d("SettingsFragment", "Total unresolved questions: $totalQuestions")

                tvQuestionCountBadge.text = "$totalQuestions вопросов"
                btnStartTraining.isEnabled = totalQuestions > 0
                
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Exception while loading training sessions", e)
                tvQuestionCountBadge.text = "Ошибка загрузки"
                btnStartTraining.isEnabled = false
            }
        }
    }
}