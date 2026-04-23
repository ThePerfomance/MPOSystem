package com.example.groupprojectfirsttry.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.simpleClasses.Answer
import com.example.groupprojectfirsttry.simpleClasses.TrainingQuestion
import com.example.groupprojectfirsttry.simpleClasses.TrainingSession
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch

class TrainingFragment : Fragment(R.layout.fragment_training) {

    private lateinit var session: TrainingSession
    private var questions: List<TrainingQuestion> = emptyList()
    private val selectedAnswers = mutableMapOf<Int, Answer>() // trainingQuestionId -> chosen answer
    private var correctAnswersCount = 0
    private var isFinished = false

    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var llQuestionsContainer: LinearLayout
    private lateinit var llFeedback: CardView
    private lateinit var tvFeedbackTitle: TextView
    private lateinit var tvCorrectAnswersCount: TextView
    private lateinit var btnAction: MaterialButton
    private lateinit var btnBackHeader: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = arguments?.getParcelable("session") ?: return
        
        // В работу над ошибками попадают только неверно отвеченные вопросы
        questions = session.questions?.filter { it.status != "correct" } ?: emptyList()

        if (questions.isEmpty()) {
            Toast.makeText(context, "Все ошибки исправлены!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Initialize views
        tvProgress = view.findViewById(R.id.tvProgress)
        progressBar = view.findViewById(R.id.progressBar)
        llQuestionsContainer = view.findViewById(R.id.llQuestionsContainer)
        llFeedback = view.findViewById(R.id.llFeedback)
        tvFeedbackTitle = view.findViewById(R.id.tvFeedbackTitle)
        tvCorrectAnswersCount = view.findViewById(R.id.tvCorrectAnswersCount)
        btnAction = view.findViewById(R.id.btnAction)
        btnBackHeader = view.findViewById(R.id.btnBackHeader)

        btnBackHeader.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnAction.setOnClickListener {
            if (isFinished) {
                parentFragmentManager.popBackStack()
            } else {
                if (selectedAnswers.size == questions.size) {
                    submitAllAnswers()
                } else {
                    Toast.makeText(context, "Ответьте на все вопросы", Toast.LENGTH_SHORT).show()
                }
            }
        }

        updateProgressHeader(0)
        renderQuestions()
    }

    private fun renderQuestions() {
        if (!isAdded) return
        llQuestionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        questions.forEachIndexed { index, trainingQuestion ->
            val question = trainingQuestion.question ?: return@forEachIndexed
            val questionView = inflater.inflate(R.layout.item_test_question, llQuestionsContainer, false)
            
            questionView.findViewById<TextView>(R.id.tvQuestionNumber).text = "Вопрос ${index + 1}"
            questionView.findViewById<TextView>(R.id.tvQuestionText).text = question.text
            
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

            rgAnswers.setOnCheckedChangeListener { group, checkedId ->
                val checkedRb = group.findViewById<RadioButton>(checkedId)
                if (checkedRb != null) {
                    // Анимация выбора
                    checkedRb.animate().scaleX(1.03f).scaleY(1.03f).setDuration(100).withEndAction {
                        checkedRb.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()

                    val selectedAnswer = checkedRb.tag as Answer
                    selectedAnswers[trainingQuestion.id] = selectedAnswer
                    updateSubmitButtonState()
                    updateProgressHeader(selectedAnswers.size)
                }
            }

            llQuestionsContainer.addView(questionView)
        }
    }

    private fun updateProgressHeader(answeredCount: Int) {
        val total = questions.size
        tvProgress.text = "$answeredCount / $total"
        
        progressBar.max = total * 100
        val targetProgress = answeredCount * 100

        android.animation.ObjectAnimator.ofInt(progressBar, "progress", targetProgress)
            .setDuration(500)
            .apply {
                interpolator = android.view.animation.DecelerateInterpolator()
                start()
            }
    }

    private fun updateSubmitButtonState() {
        val isAllAnswered = selectedAnswers.size == questions.size
        btnAction.isEnabled = isAllAnswered
        btnAction.backgroundTintList = ColorStateList.valueOf(
            if (isAllAnswered) Color.parseColor("#0A0B0E") else Color.parseColor("#8E8E93")
        )
    }

    private fun submitAllAnswers() {
        lifecycleScope.launch {
            btnAction.isEnabled = false
            btnAction.text = "Загрузка..."
            
            var correctCount = 0
            
            questions.forEach { tq ->
                val selectedAnswerId = selectedAnswers[tq.id]?.id ?: return@forEach
                try {
                    val response = ApiClient.apiService.submitTrainingAnswer(
                        tq.id, 
                        mapOf("chosen_answer_id" to selectedAnswerId)
                    )
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.isCorrect == true || body?.status?.lowercase() == "correct") {
                            correctCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TrainingFragment", "Error submitting answer for ${tq.id}", e)
                }
            }
            
            correctAnswersCount = correctCount
            showFinalResults()
        }
    }

    private fun showFinalResults() {
        isFinished = true
        llFeedback.visibility = View.VISIBLE
        
        val total = questions.size
        tvCorrectAnswersCount.text = "Правильных ответов: $correctAnswersCount из $total"
        
        if (correctAnswersCount == total) {
            tvFeedbackTitle.text = "✅ Отличный результат!"
            llFeedback.setCardBackgroundColor(Color.parseColor("#F1FFF1"))
        } else {
            tvFeedbackTitle.text = "📚 Попробуйте ещё раз"
            llFeedback.setCardBackgroundColor(Color.parseColor("#FFF1F1"))
        }

        btnAction.text = "Завершить"
        btnAction.isEnabled = true
        btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0A0B0E"))

        // Блокируем выбор
        for (i in 0 until llQuestionsContainer.childCount) {
            val rg = llQuestionsContainer.getChildAt(i).findViewById<RadioGroup>(R.id.rgAnswers)
            for (j in 0 until rg.childCount) {
                rg.getChildAt(j).isEnabled = false
            }
        }
    }
}
