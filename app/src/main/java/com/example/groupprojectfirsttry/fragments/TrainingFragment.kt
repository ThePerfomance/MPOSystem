package com.example.groupprojectfirsttry.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.BuildConfig
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.AdaptiveTrainingRequest
import com.example.groupprojectfirsttry.api.SubmitTrainingAnswerRequest
import com.example.groupprojectfirsttry.simpleClasses.Answer
import com.example.groupprojectfirsttry.simpleClasses.TrainingQuestion
import com.example.groupprojectfirsttry.simpleClasses.TrainingSession
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch

class TrainingFragment : Fragment(R.layout.fragment_training) {

    private lateinit var session: TrainingSession
    private var questions: List<TrainingQuestion> = emptyList()
    private var currentQuestionIndex = 0
    private var correctAnswersCount = 0
    private var isFinished = false
    private var isAdaptive = false
    private var isQuestionAnswered = false
    private val gson = Gson()

    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var llQuestionsContainer: LinearLayout
    private lateinit var llFeedback: CardView
    private lateinit var tvFeedbackTitle: TextView
    private lateinit var tvCorrectAnswersCount: TextView
    private lateinit var btnAction: MaterialButton
    private lateinit var btnBackHeader: View
    
    private lateinit var shimmerTrainingPass: ShimmerFrameLayout
    private lateinit var llTrainingMainContent: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("DEPRECATION")
        session = arguments?.getParcelable("session") ?: return
        isAdaptive = arguments?.getBoolean("is_adaptive", false) ?: false
        
        tvProgress = view.findViewById(R.id.tvProgress)
        progressBar = view.findViewById(R.id.progressBar)
        llQuestionsContainer = view.findViewById(R.id.llQuestionsContainer)
        llFeedback = view.findViewById(R.id.llFeedback)
        tvFeedbackTitle = view.findViewById(R.id.tvFeedbackTitle)
        tvCorrectAnswersCount = view.findViewById(R.id.tvCorrectAnswersCount)
        btnAction = view.findViewById(R.id.btnAction)
        btnBackHeader = view.findViewById(R.id.btnBackHeader)
        
        shimmerTrainingPass = view.findViewById(R.id.shimmer_training_pass)
        llTrainingMainContent = view.findViewById(R.id.llTrainingMainContent)

        btnBackHeader.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnAction.setOnClickListener {
            onActionClicked()
        }

        loadSessionData()
    }

    private fun startLoading() {
        shimmerTrainingPass.isVisible = true
        shimmerTrainingPass.startShimmer()
        llTrainingMainContent.isVisible = false
    }

    private fun stopLoading() {
        shimmerTrainingPass.stopShimmer()
        shimmerTrainingPass.isVisible = false
        llTrainingMainContent.isVisible = true
    }

    private fun onActionClicked() {
        if (isFinished) {
            if (isAdaptive) {
                repeatAdaptiveTraining()
            } else {
                parentFragmentManager.popBackStack()
            }
            return
        }

        if (!isQuestionAnswered) {
            val currentTQ = questions.getOrNull(currentQuestionIndex) ?: return
            val selectedIds = getSelectedAnswers()
            if (selectedIds.isNotEmpty()) {
                submitAdaptiveAnswers(currentTQ, selectedIds)
            } else {
                Toast.makeText(context, "Выберите хотя бы один вариант ответа", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                isQuestionAnswered = false
                renderCurrentQuestion()
                updateProgressHeader(currentQuestionIndex)
            } else {
                showFinalResults()
            }
        }
    }

    private fun getSelectedAnswers(): List<Int> {
        val questionView = llQuestionsContainer.getChildAt(0) ?: return emptyList()
        val container = questionView.findViewById<LinearLayout>(R.id.llAnswersContainer)
        val selectedIds = mutableListOf<Int>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is CompoundButton && child.isChecked) {
                (child.tag as? Answer)?.let { selectedIds.add(it.id) }
            }
        }
        Log.d("JSON_LOG", "getSelectedAnswers -> chosen_answers: ${gson.toJson(selectedIds)}")
        return selectedIds
    }

    private fun loadSessionData() {
        startLoading()
        
        questions = if (isAdaptive) {
            session.questions ?: emptyList()
        } else {
            session.questions?.filter { it.status != "correct" } ?: emptyList()
        }

        if (questions.isEmpty()) {
            Toast.makeText(context, "Нет вопросов для тренировки", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        currentQuestionIndex = 0
        isQuestionAnswered = false
        correctAnswersCount = 0
        renderCurrentQuestion()
        updateProgressHeader(currentQuestionIndex)
        
        stopLoading()
    }

    private fun renderCurrentQuestion() {
        if (!isAdded) return
        llQuestionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        
        val trainingQuestion = questions.getOrNull(currentQuestionIndex) ?: return
        val question = trainingQuestion.question ?: return
        val questionView = inflater.inflate(R.layout.item_test_question, llQuestionsContainer, false)
        
        questionView.findViewById<TextView>(R.id.tvQuestionNumber).text = "Карточка ${currentQuestionIndex + 1}"
        questionView.findViewById<TextView>(R.id.tvQuestionText).text = question.text
        
        val isMultipleChoice = question.isMultipleChoice
        Log.d("JSON_LOG", "renderCurrentQuestion -> is_multiple_choice: $isMultipleChoice, Question JSON: ${gson.toJson(question)}")
        
        val tvMultipleChoiceHint = questionView.findViewById<TextView>(R.id.tvMultipleChoiceHint)
        tvMultipleChoiceHint.isVisible = isMultipleChoice
        
        // Difficulty badge
        val tvDifficulty = questionView.findViewById<TextView>(R.id.tvDifficultyBadge)
        val diff = question.difficulty?.level?.lowercase()
        if (diff != null && BuildConfig.SHOW_DIFFICULTY_AND_RATING) {
            tvDifficulty.isVisible = true
            tvDifficulty.text = when(diff) {
                "easy" -> "Легкий"
                "medium" -> "Средний"
                "hard" -> "Сложный"
                else -> question.difficulty?.level ?: ""
            }
            val colorRes = when(diff) {
                "easy" -> R.color.DifficultyEasy
                "medium" -> R.color.DifficultyMedium
                "hard" -> R.color.DifficultyHard
                else -> R.color.SecondaryTextColor
            }
            tvDifficulty.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes))
        } else {
            tvDifficulty.isVisible = false
        }

        val llAnswersContainer = questionView.findViewById<LinearLayout>(R.id.llAnswersContainer)
        val optionViews = mutableListOf<CompoundButton>()

        question.answers.forEach { answer ->
            val optionView = if (isMultipleChoice) {
                CheckBox(requireContext())
            } else {
                RadioButton(requireContext())
            }

            optionView.apply {
                text = answer.text
                id = View.generateViewId()
                tag = answer
                textSize = 17f
                setPadding(32, 28, 32, 28)
                compoundDrawablePadding = 24
                gravity = Gravity.CENTER_VERTICAL
                setButtonDrawable(null)
                
                val drawableRes = if (isMultipleChoice) R.drawable.bg_checkbox_custom else R.drawable.bg_radio_button_custom
                setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, drawableRes),
                    null, null, null
                )
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(12, 12, 12, 12)
                layoutParams = params
                setBackgroundResource(R.drawable.bg_answer_item_selector)
                
                setOnCheckedChangeListener { buttonView, isChecked ->
                    if (!isMultipleChoice && isChecked) {
                        optionViews.forEach { if (it != buttonView) (it as? RadioButton)?.isChecked = false }
                    }
                    
                    btnAction.isEnabled = optionViews.any { it.isChecked }
                    btnAction.backgroundTintList = ColorStateList.valueOf(
                        if (btnAction.isEnabled) "#0A0B0E".toColorInt() else "#8E8E93".toColorInt()
                    )

                    // Анимация выбора
                    buttonView.animate()
                        .scaleX(1.03f)
                        .scaleY(1.03f)
                        .setDuration(100)
                        .withEndAction {
                            buttonView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }
                        .start()
                    
                    Log.d("JSON_LOG", "TrainingFragment -> Answer Toggled: ID=${answer.id}, Checked=$isChecked, isMultipleChoice=$isMultipleChoice")
                }
            }
            llAnswersContainer.addView(optionView)
            optionViews.add(optionView)
        }

        btnAction.text = "Проверить ответ"
        btnAction.backgroundTintList = ColorStateList.valueOf("#8E8E93".toColorInt())
        btnAction.isEnabled = false

        llQuestionsContainer.addView(questionView)
    }

    private fun submitAdaptiveAnswers(tq: TrainingQuestion, chosenIds: List<Int>) {
        lifecycleScope.launch {
            btnAction.isEnabled = false
            btnAction.text = "Загрузка..."
            
            try {
                val requestBody = SubmitTrainingAnswerRequest(chosenAnswers = chosenIds)
                Log.d("JSON_LOG", "submitAdaptiveAnswers -> request JSON: ${gson.toJson(requestBody)}")
                
                val response = ApiClient.apiService.submitTrainingAnswer(
                    tq.id, 
                    requestBody
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("JSON_LOG", "submitAdaptiveAnswers -> response JSON: ${gson.toJson(body)}")
                    
                    val isCorrect = body?.isCorrect == true || body?.status?.lowercase() == "correct"
                    if (isCorrect) correctAnswersCount++
                    
                    val correctIds = body?.correctAnswerIds ?: listOfNotNull(body?.correctAnswerId)
                    showFeedback(chosenIds, correctIds, body?.explanation, isCorrect)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("TrainingFragment", "Server Error: $errorBody")
                    Toast.makeText(context, "Ошибка сервера", Toast.LENGTH_SHORT).show()
                    btnAction.isEnabled = true
                    btnAction.text = "Проверить ответ"
                }
            } catch (e: Exception) {
                Log.e("TrainingFragment", "Exception: ${e.message}", e)
                Toast.makeText(context, "Ошибка соединения", Toast.LENGTH_SHORT).show()
                btnAction.isEnabled = true
                btnAction.text = "Проверить ответ"
            }
        }
    }

    private fun showFeedback(selectedIds: List<Int>, correctIds: List<Int>, explanation: String?, isCorrect: Boolean) {
        isQuestionAnswered = true
        val questionView = llQuestionsContainer.getChildAt(0) ?: return
        val container = questionView.findViewById<LinearLayout>(R.id.llAnswersContainer)
        
        // Проверяем, является ли ответ неполным (выбраны только верные, но не все)
        val isAnyIncorrectSelected = selectedIds.any { !correctIds.contains(it) }
        val isAllCorrectSelected = correctIds.all { selectedIds.contains(it) }
        val isIncomplete = !isAnyIncorrectSelected && !isAllCorrectSelected && selectedIds.isNotEmpty()

        Log.d("JSON_LOG", "showFeedback -> selectedIds: ${gson.toJson(selectedIds)}, correctIds: ${gson.toJson(correctIds)}, isIncomplete: $isIncomplete")

        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i) as? CompoundButton ?: continue
            view.isEnabled = false
            val answer = view.tag as Answer
            val isSelected = selectedIds.contains(answer.id)
            val isActuallyCorrect = correctIds.contains(answer.id)
            
            if (isSelected) {
                if (isActuallyCorrect) {
                    if (isIncomplete) {
                        // Оранжевый для неполного ответа
                        view.setBackgroundResource(R.drawable.bg_result_summary_orange)
                        view.setTextColor("#E65100".toColorInt())
                    } else {
                        // Зеленый для полностью правильного
                        view.setBackgroundResource(R.drawable.bg_result_summary_green)
                        view.setTextColor("#1B5E20".toColorInt())
                    }
                } else {
                    // Красный для неправильного
                    view.setBackgroundResource(R.drawable.bg_feedback_incorrect)
                    view.setTextColor("#B71C1C".toColorInt())
                }
            }
            // Варианты, которые пользователь не выбрал, остаются без пометок (даже если они правильные)
        }
        
        val llExpl = questionView.findViewById<View>(R.id.llExplanation)
        val tvExpl = questionView.findViewById<TextView>(R.id.tvExplanationText)
        
        var finalExplanation = explanation ?: ""
        if (isIncomplete) {
            finalExplanation = if (finalExplanation.isEmpty()) "Ответ неполный." else "Ответ неполный. $finalExplanation"
        }

        if (finalExplanation.isNotBlank()) {
            llExpl.isVisible = true
            tvExpl.text = finalExplanation
        }

        btnAction.isEnabled = true
        btnAction.text = if (currentQuestionIndex < questions.size - 1) "Следующий вопрос" else "Завершить"
        btnAction.backgroundTintList = ColorStateList.valueOf("#0A0B0E".toColorInt())
    }

    private fun updateProgressHeader(index: Int) {
        val total = questions.size
        if (total == 0) return
        tvProgress.text = "${index + 1} / $total"
        progressBar.max = total * 100
        val targetProgress = (index + 1) * 100
        android.animation.ObjectAnimator.ofInt(progressBar, "progress", targetProgress)
            .setDuration(500).start()
    }

    private fun showFinalResults() {
        isFinished = true
        llFeedback.isVisible = true
        llQuestionsContainer.removeAllViews()
        
        val total = questions.size
        tvCorrectAnswersCount.text = "Вы успешно разобрали $total карточек"
        tvFeedbackTitle.text = "✨ Тренировка завершена!"
        llFeedback.setCardBackgroundColor("#F1FFF1".toColorInt())

        if (isAdaptive) {
            btnAction.text = "Повторить тренировку"
        } else {
            btnAction.text = "Завершить"
        }
        btnAction.isEnabled = true
        btnAction.backgroundTintList = ColorStateList.valueOf("#0A0B0E".toColorInt())
    }

    private fun repeatAdaptiveTraining() {
        startLoading()
        lifecycleScope.launch {
            try {
                val request = AdaptiveTrainingRequest(
                    lessonId = session.lessonId?.toString(),
                    onlyPassed = true,
                    excludeCorrect = true
                )
                
                val response = ApiClient.apiService.createAdaptiveTrainingSession(request)
                if (response.isSuccessful) {
                    val newSession = response.body()?.session
                    if (newSession != null) {
                        session = newSession
                        llFeedback.isVisible = false
                        isFinished = false
                        loadSessionData()
                    } else {
                        parentFragmentManager.popBackStack()
                    }
                } else {
                    parentFragmentManager.popBackStack()
                }
            } catch (e: Exception) {
                parentFragmentManager.popBackStack()
            }
        }
    }
}
