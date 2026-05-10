package com.example.groupprojectfirsttry.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.simpleClasses.Answer
import com.example.groupprojectfirsttry.simpleClasses.TrainingQuestion
import com.example.groupprojectfirsttry.simpleClasses.TrainingSession
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class TrainingFragment : Fragment(R.layout.fragment_training) {

    private lateinit var session: TrainingSession
    private var questions: List<TrainingQuestion> = emptyList()
    private var currentQuestionIndex = 0
    private var correctAnswersCount = 0
    private var isFinished = false
    private var isAdaptive = false
    private var isQuestionAnswered = false

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
            val selectedAnswer = getSelectedAnswerForCurrent()
            if (selectedAnswer != null) {
                submitAdaptiveAnswer(currentTQ, selectedAnswer)
            } else {
                Toast.makeText(context, "Выберите вариант ответа", Toast.LENGTH_SHORT).show()
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

    private fun getSelectedAnswerForCurrent(): Answer? {
        val questionView = llQuestionsContainer.getChildAt(0) ?: return null
        val rg = questionView.findViewById<RadioGroup>(R.id.rgAnswers)
        val checkedId = rg.checkedRadioButtonId
        if (checkedId == -1) return null
        return rg.findViewById<RadioButton>(checkedId).tag as? Answer
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
        
        // Difficulty badge
        val tvDifficulty = questionView.findViewById<TextView>(R.id.tvDifficultyBadge)
        val diff = question.difficulty?.lowercase()
        if (diff != null) {
            tvDifficulty.isVisible = true
            tvDifficulty.text = when(diff) {
                "easy" -> "Легкий"
                "medium" -> "Средний"
                "hard" -> "Сложный"
                else -> question.difficulty
            }
            val color = when(diff) {
                "easy" -> "#4CAF50"
                "medium" -> "#FF9800"
                "hard" -> "#F44336"
                else -> "#8E8E93"
            }
            tvDifficulty.backgroundTintList = ColorStateList.valueOf(color.toColorInt())
        } else {
            tvDifficulty.isVisible = false
        }

        val rgAnswers = questionView.findViewById<RadioGroup>(R.id.rgAnswers)
        
        question.answers.forEach { answer ->
            val rb = RadioButton(requireContext()).apply {
                text = answer.text
                id = View.generateViewId()
                tag = answer
                textSize = 17f
                setPadding(32, 28, 32, 28)
                compoundDrawablePadding = 24
                gravity = Gravity.CENTER_VERTICAL
                setButtonDrawable(null)
                setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.bg_radio_button_custom),
                    null, null, null
                )
                
                val params = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(12, 12, 12, 12)
                layoutParams = params
                setBackgroundResource(R.drawable.bg_answer_item_selector)
            }
            rgAnswers.addView(rb)
        }

        btnAction.text = "Проверить ответ"
        btnAction.backgroundTintList = ColorStateList.valueOf("#8E8E93".toColorInt())
        btnAction.isEnabled = false

        rgAnswers.setOnCheckedChangeListener { _, _ ->
            btnAction.isEnabled = true
            btnAction.backgroundTintList = ColorStateList.valueOf("#0A0B0E".toColorInt())
        }

        llQuestionsContainer.addView(questionView)
    }

    private fun submitAdaptiveAnswer(tq: TrainingQuestion, selectedAnswer: Answer) {
        lifecycleScope.launch {
            btnAction.isEnabled = false
            btnAction.text = "Загрузка..."
            
            try {
                val response = ApiClient.apiService.submitTrainingAnswer(
                    tq.id, 
                    mapOf("chosen_answer_id" to selectedAnswer.id)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val isCorrect = body?.isCorrect == true || body?.status?.lowercase() == "correct"
                    if (isCorrect) correctAnswersCount++
                    
                    showFeedback(selectedAnswer.id, body?.correctAnswerId, body?.explanation, isCorrect)
                } else {
                    btnAction.isEnabled = true
                    btnAction.text = "Проверить ответ"
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка соединения", Toast.LENGTH_SHORT).show()
                btnAction.isEnabled = true
                btnAction.text = "Проверить ответ"
            }
        }
    }

    private fun showFeedback(selectedId: Int, correctId: Int?, explanation: String?, isCorrect: Boolean) {
        isQuestionAnswered = true
        val questionView = llQuestionsContainer.getChildAt(0) ?: return
        val rg = questionView.findViewById<RadioGroup>(R.id.rgAnswers)
        
        for (i in 0 until rg.childCount) {
            val rb = rg.getChildAt(i) as RadioButton
            rb.isEnabled = false
            val answer = rb.tag as Answer
            
            if (answer.id == correctId || (isCorrect && answer.id == selectedId)) {
                rb.setBackgroundResource(R.drawable.bg_result_summary_green)
                rb.setTextColor("#1B5E20".toColorInt())
            } else if (answer.id == selectedId && !isCorrect) {
                rb.setBackgroundResource(R.drawable.bg_feedback_incorrect)
                rb.setTextColor("#B71C1C".toColorInt())
            }
        }
        
        val llExpl = questionView.findViewById<View>(R.id.llExplanation)
        val tvExpl = questionView.findViewById<TextView>(R.id.tvExplanationText)
        if (!explanation.isNullOrBlank()) {
            llExpl.isVisible = true
            tvExpl.text = explanation
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
                val response = ApiClient.apiService.createAdaptiveTrainingSession()
                if (response.isSuccessful && isAdded) {
                    val newSession = response.body()?.session
                    if (newSession != null) {
                        session = newSession
                        currentQuestionIndex = 0
                        correctAnswersCount = 0
                        isFinished = false
                        isQuestionAnswered = false
                        llFeedback.isVisible = false
                        loadSessionData()
                    } else {
                        stopLoading()
                    }
                } else {
                    stopLoading()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                stopLoading()
            }
        }
    }
}
