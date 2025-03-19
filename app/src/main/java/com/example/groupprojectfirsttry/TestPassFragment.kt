package com.example.groupprojectfirsttry

import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TestPassFragment : Fragment(R.layout.fragment_test_pass) {

    private lateinit var test: Test
    private var questions = emptyList<Question>()
    private var currentQuestionIndex = 0
    private lateinit var answersAdapter: AnswersAdapter
    private val selectedAnswers = mutableMapOf<Int, Answer>() // Хранит questionId → выбранный ответ

    // Интерфейс для получения пользователя
    private lateinit var userProvider: UserProvider
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Получаем User из аргументов, если они есть
        user = requireArguments().getParcelable("user") ?: throw IllegalArgumentException("User not found")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UserProvider) {
            userProvider = context
            user = userProvider.getUser() // Получаем пользователя из активности
        } else {
            throw RuntimeException("$context must implement UserProvider")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получите тест из аргументов
        test = requireArguments().getParcelable("test") ?: throw IllegalArgumentException("Test not found")

        // Инициализация RecyclerView
        val answersList = view.findViewById<RecyclerView>(R.id.answersList)
        answersList.layoutManager = LinearLayoutManager(context)

        // Настройка кнопки "Далее"
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener {
            currentQuestionIndex++
            updateQuestion()
        }

        // Загрузка вопросов
        loadQuestions(test.id)
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
                    else -> Toast.makeText(context, "Ошибка: ${e.code()}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateQuestion() {
        if (currentQuestionIndex >= questions.size) {
            finishTest()
            return
        }

        val question = questions[currentQuestionIndex]
        view?.findViewById<TextView>(R.id.tvQuestion)?.text = question.text

        answersAdapter = AnswersAdapter(
            question.answers,
            onAnswerSelected = { answer ->
                selectedAnswers[question.id] = answer // Сохраняем выбранный ответ
            }
        )
        view?.findViewById<RecyclerView>(R.id.answersList)?.adapter = answersAdapter

        // Обновляем текст кнопки
        val btnNext = view?.findViewById<Button>(R.id.btnNext)
        btnNext?.text = if (currentQuestionIndex == questions.lastIndex) {
            "Завершить"
        } else {
            "Далее"
        }
    }

    private fun finishTest() {
        if (questions.isEmpty()) return // Защита от пустого теста

        // Проверяем, что все вопросы имеют выбранные ответы
        for ((questionId, _) in questions.withIndex()) {
            if (selectedAnswers[questions[questionId].id] == null) {
                Toast.makeText(context, "Выберите ответ для всех вопросов", Toast.LENGTH_SHORT).show()
                currentQuestionIndex = questionId // Возвращаемся к вопросу без ответа
                updateQuestion()
                return
            }
        }

        // Рассчитываем score
        val score = calculateScore()

        // Показываем результат
        showResultDialog(score)

        // Отправляем результаты на сервер
        sendResultsToServer(score)
    }

    private fun calculateScore(): Int {
        var score = 0
        for ((questionId, answer) in selectedAnswers) {
            if (answer.isCorrect) score++
        }
        return score
    }

    private fun showResultDialog(score: Int) {
        val message = "Вы набрали $score из ${questions.size} баллов!"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun sendResultsToServer(score: Int) {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val userId = user.id ?: throw IllegalStateException("User ID is null")

        val testResult = TestResult(
            user_id = userId,
            test_id = test.id,
            score = score,
            completed_at = currentTime
        )

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.submitTestResult(testResult)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Результаты отправлены!", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                } else {
                    Toast.makeText(
                        context,
                        "Ошибка отправки: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}