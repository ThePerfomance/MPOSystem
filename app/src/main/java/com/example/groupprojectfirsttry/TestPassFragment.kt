package com.example.groupprojectfirsttry

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.HttpException

class TestPassFragment : Fragment(R.layout.fragment_test_pass) {

    private lateinit var test: Test
    private var questions = emptyList<Question>()
    private var currentQuestionIndex = 0
    private lateinit var answersAdapter: AnswersAdapter
    private val selectedAnswers = mutableListOf<Answer>()

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
            Log.d("API", "Запрос вопросов для теста: $testId")
            val response = ApiClient.apiService.getQuestions(testId)
            if (response.isNotEmpty()) { // Если список не пустой
                questions = response
                updateQuestion()
            } else {
                Toast.makeText(context, "Нет вопросов для этого теста", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            if (e is HttpException) {
                when (e.code()) {
                    404 -> Toast.makeText(context, "Ошибка 404: Тест не найден", Toast.LENGTH_SHORT).show()
                    400 -> Toast.makeText(context, "Ошибка 400: Неверный ID теста", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Произошла ошибка", Toast.LENGTH_SHORT).show()
            }
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
        answersAdapter = AnswersAdapter(
            question.answers, // Список ответов
            onAnswerSelected = { answer ->
                // Добавляем выбранный ответ в список
                selectedAnswers.add(answer)
            }
        )
        view?.findViewById<RecyclerView>(R.id.answersList)?.adapter = answersAdapter
    }
    private fun finishTest() {
        // Отправьте результаты на сервер или сохраните их
        // Пример отправки:
        lifecycleScope.launch {
            try {
                ApiClient.apiService.submitTestResult(TestResult(
                    testId = test.id,
                    answers = selectedAnswers
                ))
                Toast.makeText(context, "Тест пройден!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}