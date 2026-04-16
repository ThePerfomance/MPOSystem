package com.example.groupprojectfirsttry.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.adapters.AnswersAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.simpleClasses.TrainingQuestion
import com.example.groupprojectfirsttry.simpleClasses.TrainingSession
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.UUID

class TrainingFragment : Fragment(R.layout.fragment_training) {

    private lateinit var session: TrainingSession
    private var questions: MutableList<TrainingQuestion> = mutableListOf()
    private var currentIndex = 0
    private var correctAnswersCount = 0
    private var isFinished = false

    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvQuestionNumber: TextView
    private lateinit var tvQuestionText: TextView
    private lateinit var rvAnswers: RecyclerView
    private lateinit var llFeedback: LinearLayout
    private lateinit var tvFeedbackTitle: TextView
    private lateinit var tvCorrectAnswersCount: TextView
    private lateinit var btnAction: MaterialButton
    private lateinit var btnBackHeader: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = arguments?.getParcelable("session") ?: return
        
        Log.d("TrainingFragment", "Training Session JSON: ${Gson().toJson(session)}")

        // В работу над ошибками попадают только неверно отвеченные вопросы
        questions = session.questions?.filter { it.status != "correct" }?.toMutableList() ?: mutableListOf()

        if (questions.isEmpty()) {
            Toast.makeText(context, "Все ошибки исправлены!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Initialize views
        tvProgress = view.findViewById(R.id.tvProgress)
        progressBar = view.findViewById(R.id.progressBar)
        tvQuestionNumber = view.findViewById(R.id.tvQuestionNumber)
        tvQuestionText = view.findViewById(R.id.tvQuestionText)
        rvAnswers = view.findViewById(R.id.rvAnswers)
        llFeedback = view.findViewById(R.id.llFeedback)
        tvFeedbackTitle = view.findViewById(R.id.tvFeedbackTitle)
        tvCorrectAnswersCount = view.findViewById(R.id.tvCorrectAnswersCount)
        btnAction = view.findViewById(R.id.btnAction)
        btnBackHeader = view.findViewById(R.id.btnBackHeader)

        rvAnswers.layoutManager = LinearLayoutManager(context)
        
        btnBackHeader.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnAction.setOnClickListener {
            if (isFinished) {
                parentFragmentManager.popBackStack()
            } else {
                handleActionClick()
            }
        }

        showQuestion()
    }

    private fun showQuestion() {
        val trainingQuestion = questions[currentIndex]
        val question = trainingQuestion.question
        
        if (question == null) {
            skipQuestion()
            return
        }

        val progressText = "${currentIndex + 1} / ${questions.size}"
        tvProgress.text = progressText
        progressBar.max = questions.size
        progressBar.progress = currentIndex + 1

        tvQuestionNumber.text = "Вопрос ${currentIndex + 1}"
        tvQuestionText.text = question.text
        tvQuestionText.visibility = View.VISIBLE
        
        llFeedback.visibility = View.GONE
        rvAnswers.visibility = View.VISIBLE
        
        btnAction.text = "Проверить ответ"
        btnAction.setBackgroundColor(resources.getColor(R.color.OnboardingSecondaryTextColor, null))

        val adapter = AnswersAdapter(question.answers) { 
            // Кнопка визуально не меняется до нажатия
        }
        rvAnswers.adapter = adapter
    }

    private fun skipQuestion() {
        currentIndex++
        if (currentIndex < questions.size) showQuestion() else showFinalResults()
    }

    private fun handleActionClick() {
        val adapter = rvAnswers.adapter as? AnswersAdapter
        val selectedAnswer = adapter?.getSelectedAnswer()
        
        if (selectedAnswer == null) {
            Toast.makeText(context, "Выберите ответ", Toast.LENGTH_SHORT).show()
            return
        }

        submitAndProceed(questions[currentIndex].id, selectedAnswer.id)
    }

    private fun submitAndProceed(trainingQuestionId: Int, answerId: Int) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.submitTrainingAnswer(
                    trainingQuestionId, 
                    mapOf("chosen_answer_id" to answerId)
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d("TrainingFragment", "Answer Response JSON: ${Gson().toJson(body)}")
                    
                    // Учитываем и флаг, и строковый статус для надежности.
                    // Используем trim и lowercase для защиты от особенностей Django
                    val isCorrect = body.isCorrect == true || body.status?.trim()?.lowercase() == "correct"
                    
                    if (isCorrect) {
                        correctAnswersCount++
                        Log.d("TrainingFragment", "Correct! Counter: $correctAnswersCount")
                    }

                    if (currentIndex < questions.size - 1) {
                        currentIndex++
                        showQuestion()
                    } else {
                        showFinalResults()
                    }
                }
            } catch (e: Exception) {
                Log.e("TrainingFragment", "Error", e)
            }
        }
    }

    private fun showFinalResults() {
        isFinished = true
        
        tvQuestionText.visibility = View.GONE
        rvAnswers.visibility = View.GONE
        
        val isAllCorrect = correctAnswersCount == questions.size
        
        tvFeedbackTitle.visibility = View.VISIBLE
        if (isAllCorrect) {
            tvFeedbackTitle.text = "Работа над ошибками пройдена! ✅"
            tvFeedbackTitle.setTextColor(resources.getColor(R.color.GraphicCorrectColor, null))
        } else {
            tvFeedbackTitle.text = "Работа над ошибками не зачтена! ❌"
            tvFeedbackTitle.setTextColor(resources.getColor(R.color.TestRedAccent, null))
        }

        tvCorrectAnswersCount.text = "Правильных ответов: $correctAnswersCount из ${questions.size}"
        tvCorrectAnswersCount.visibility = View.VISIBLE
        llFeedback.visibility = View.VISIBLE
        
        btnAction.text = "Завершить"
        btnAction.setBackgroundColor(resources.getColor(R.color.AccentColor, null))
        progressBar.progress = questions.size
    }
}
