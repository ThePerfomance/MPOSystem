package com.example.groupprojectfirsttry

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class TestPassFragment : Fragment(R.layout.fragment_test_pass) {

    private lateinit var test: Test
    private lateinit var questions: List<Question>
    private var currentQuestionIndex = 0
    private lateinit var answersAdapter: AnswersAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получите тест из аргументов
        test = requireArguments().getParcelable("test") ?: throw IllegalArgumentException("Test not found")

        // Инициализация RecyclerView
        val answersList = view.findViewById<RecyclerView>(R.id.answersList)
        answersList.layoutManager = LinearLayoutManager(context)

        // Загрузка вопросов
        loadQuestions(test.id)

        // Настройка кнопки "Далее"
        view.findViewById<Button>(R.id.btnNext).setOnClickListener {
            currentQuestionIndex++
            updateQuestion()
        }
    }

    private fun loadQuestions(testId: Int) = lifecycleScope.launch {
        try {
            questions = ApiClient.apiService.getQuestions(testId)
            updateQuestion()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateQuestion() {
        if (currentQuestionIndex >= questions.size) {
            // Тест завершен
            Toast.makeText(context, "Тест завершен!", Toast.LENGTH_SHORT).show()
            return
        }

        val question = questions[currentQuestionIndex]
        view?.findViewById<TextView>(R.id.tvQuestion)?.text = question.text

        // Обновление адаптера
        answersAdapter = AnswersAdapter(question.answers) { answer ->
            // Обработка выбора ответа
        }
        view?.findViewById<RecyclerView>(R.id.answersList)?.adapter = answersAdapter
    }
}