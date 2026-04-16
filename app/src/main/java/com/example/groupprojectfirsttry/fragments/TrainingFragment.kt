package com.example.groupprojectfirsttry.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
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
import kotlinx.coroutines.launch
import java.util.UUID

class TrainingFragment : Fragment(R.layout.fragment_training) {

    private lateinit var session: TrainingSession
    private var questions: MutableList<TrainingQuestion> = mutableListOf()
    private var currentIndex = 0

    private lateinit var tvQuestion: TextView
    private lateinit var rvAnswers: RecyclerView
    private lateinit var llRecommendation: View
    private lateinit var tvRecLink: TextView
    private lateinit var tvRecVideo: TextView
    private lateinit var btnNext: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = arguments?.getParcelable("session") ?: return
        
        // Фильтруем вопросы по статусу 'pending' или 'wrong' (не решенные)
        questions = session.questions?.filter { it.status != "correct" }?.toMutableList() ?: mutableListOf()

        Log.d("TrainingFragment", "Session ID: ${session.id}, Total questions: ${session.questions?.size}, To resolve: ${questions.size}")
        
        // Логируем детали вопросов для отладки
        session.questions?.forEachIndexed { index, q ->
            Log.d("TrainingFragment", "Question[$index]: id=${q.id}, status=${q.status}, hasDetails=${q.question != null}")
        }

        if (questions.isEmpty()) {
            Toast.makeText(context, "Все ошибки исправлены!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        tvQuestion = view.findViewById(R.id.tvQuestion)
        rvAnswers = view.findViewById(R.id.rvAnswers)
        llRecommendation = view.findViewById(R.id.llRecommendation)
        tvRecLink = view.findViewById(R.id.tvRecommendationLink)
        tvRecVideo = view.findViewById(R.id.tvRecommendationVideo)
        btnNext = view.findViewById(R.id.btnNext)

        rvAnswers.layoutManager = LinearLayoutManager(context)
        
        showQuestion()

        btnNext.setOnClickListener {
            currentIndex++
            if (currentIndex < questions.size) {
                showQuestion()
            } else {
                Toast.makeText(context, "Поздравляем, работа над ошибками завершена!", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun showQuestion() {
        val trainingQuestion = questions[currentIndex]
        val question = trainingQuestion.question
        
        Log.d("TrainingFragment", "Showing question at index $currentIndex: trainingQuestionId=${trainingQuestion.id}, hasQuestionDetails=${question != null}")
        
        if (question == null) {
            Log.e("TrainingFragment", "Question details are NULL for trainingQuestion ${trainingQuestion.id}")
            Toast.makeText(context, "Данные вопроса отсутствуют", Toast.LENGTH_SHORT).show()
            btnNext.visibility = View.VISIBLE
            return
        }

        Log.d("TrainingFragment", "Question text: ${question.text}, Answers count: ${question.answers.size}")

        tvQuestion.text = question.text
        llRecommendation.visibility = View.GONE
        btnNext.visibility = View.GONE

        val adapter = AnswersAdapter(question.answers) { answer ->
            submitAnswer(trainingQuestion.id, answer.id)
        }
        rvAnswers.adapter = adapter
    }

    private fun submitAnswer(trainingQuestionId: Int, answerId: Int) {
        lifecycleScope.launch {
            try {
                Log.d("TrainingFragment", "Submitting answer: trainingQuestionId=$trainingQuestionId, answerId=$answerId")
                val response = ApiClient.apiService.submitTrainingAnswer(
                    trainingQuestionId, 
                    mapOf("chosen_answer_id" to answerId)
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val isCorrect = response.body()!!.isCorrect
                    Log.d("TrainingFragment", "Submit result: isCorrect=$isCorrect")
                    
                    if (isCorrect) {
                        Toast.makeText(context, "Правильно!", Toast.LENGTH_SHORT).show()
                        btnNext.visibility = View.VISIBLE
                    } else {
                        questions[currentIndex].question?.let {
                            showRecommendations(it)
                        } ?: run {
                            Log.w("TrainingFragment", "Cannot show recommendations because question details are missing")
                            btnNext.visibility = View.VISIBLE
                        }
                    }
                } else {
                    Log.e("TrainingFragment", "Submit failed: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("TrainingFragment", "Error submitting answer", e)
            }
        }
    }

    private fun showRecommendations(question: com.example.groupprojectfirsttry.simpleClasses.Question) {
        llRecommendation.visibility = View.VISIBLE
        btnNext.visibility = View.VISIBLE
        
        if (!question.recommendationLink.isNullOrEmpty()) {
            tvRecLink.visibility = View.VISIBLE
            tvRecLink.setOnClickListener {
                openUrl(question.recommendationLink)
            }
        } else {
            tvRecLink.visibility = View.GONE
        }

        if (!question.recommendationVideoLink.isNullOrEmpty()) {
            tvRecVideo.visibility = View.VISIBLE
            tvRecVideo.setOnClickListener {
                openUrl(question.recommendationVideoLink)
            }
        } else {
            tvRecVideo.visibility = View.GONE
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
        }
    }
}
