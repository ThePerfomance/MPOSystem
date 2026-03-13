package com.example.groupprojectfirsttry.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.simpleClasses.Answer
import com.example.groupprojectfirsttry.simpleClasses.Question
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.ResultItem
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.simpleClasses.Test
import com.example.groupprojectfirsttry.simpleClasses.User
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.adapters.AnswersAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TestResult
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TestPassFragment : Fragment(R.layout.fragment_test_pass) {

    private lateinit var test: Test
    private var questions = emptyList<Question>()
    private var currentQuestionIndex = 0
    private lateinit var answersAdapter: AnswersAdapter
    private val selectedAnswers = mutableMapOf<Int, Answer>() // Хранит questionId → выбранный ответ
    private lateinit var tvUpperLeftCorner: TextView
    private lateinit var tvUpperCenter: TextView
    private lateinit var ivTestLogo: ImageView
    private lateinit var clUpHead: ConstraintLayout
    private lateinit var bnmDown: BottomNavigationView

    // Интерфейс для получения пользователя
    private lateinit var userProvider: UserProvider
    private lateinit var user: User
    //
    private lateinit var testStartTime: String // Время начала теста

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
        //Time
        testStartTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        // UI
        tvUpperLeftCorner = requireActivity().findViewById(R.id.textViewLeftUpperCorner)
        tvUpperCenter = requireActivity().findViewById(R.id.textViewUpper)
        ivTestLogo = requireActivity().findViewById(R.id.imageViewTestLogo)
        clUpHead = requireActivity().findViewById(R.id.constraintLayoutUpHead)
        bnmDown = requireActivity().findViewById(R.id.bottom_nav)

        // Настройка RecyclerView
        val answersList = view.findViewById<RecyclerView>(R.id.answersList)
        answersList.layoutManager = LinearLayoutManager(context)

        // Настройка кнопки "Далее"
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener {
            handleNextButtonClick()
        }

        // Настройка кнопки "Назад"
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            handleBackButtonClick()
        }

        // Загрузка вопросов
        loadQuestions(test.id)
    }
    private fun getCurrentTimestamp(): String {
        return java.time.LocalDateTime.now().toString() // Формат: "2023-10-05T14:48:00"
    }

    private fun loadQuestions(testId: Int) = lifecycleScope.launch {
        try {
            Log.d("API", "Запрос вопросов для теста: $testId")
            val response = ApiClient.apiService.getQuestions(testId)
            if (response.isNotEmpty()) { // Если список не пустой
                questions = response
                logQuestionsAndAnswers(questions) // Логирование вопросов и ответов
                updateQuestion()
            } else {
                Toast.makeText(context, "Нет вопросов для этого теста", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun logQuestionsAndAnswers(questions: List<Question>) {
        for (question in questions) {
            Log.d("API", "Вопрос ID: ${question.id}, Текст: ${question.text}")
            for (answer in question.answers) {
                Log.d("API", "Ответ ID: ${answer.id}, Текст: ${answer.text}, isCorrect: ${answer.is_correct}")
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
                updateNextButtonState() // Обновляем состояние кнопки
            }
        )
        view?.findViewById<RecyclerView>(R.id.answersList)?.adapter = answersAdapter

        // Обновляем текст возле кнопки
        val tvArrowTest = view?.findViewById<TextView>(R.id.textViewArrowTest)
        tvArrowTest?.text = if (currentQuestionIndex == questions.lastIndex) {
            "Завершить\nтест"
        } else {
            "Следующий\nвопрос"
        }

        // Обновляем состояние кнопок
        updateNavigationButtonsState()
    }

    private fun updateNextButtonState() {
        val btnNext = view?.findViewById<Button>(R.id.btnNext)
        val selectedAnswer = answersAdapter.getSelectedAnswer()
        btnNext?.isEnabled = selectedAnswer != null
    }

    private fun finishTest() {
        if (questions.isEmpty()) {
            Toast.makeText(context, "Тест пуст", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем, что все вопросы имеют выбранные ответы
        for (question in questions) {
            if (selectedAnswers[question.id] == null) {
                Toast.makeText(context, "Выберите ответ для всех вопросов", Toast.LENGTH_SHORT).show()
                currentQuestionIndex = questions.indexOf(question) // Возвращаемся к вопросу без ответа
                updateQuestion()
                return
            }
        }

        // Рассчитываем score
        val score = calculateScore()

        // Создаем список результатов
        val results = mutableListOf<ResultItem>()
        for (question in questions) {
            val selectedAnswer = selectedAnswers[question.id]
            val correctAnswer = question.answers.find { it.is_correct }
            val resultItem = ResultItem(
                questionText = question.text,
                answers = question.answers,
                selectedAnswerText = selectedAnswer?.text ?: "Не выбран",
                isCorrect = selectedAnswer?.id == correctAnswer?.id
            )
            results.add(resultItem)
        }

        // Отправляем результаты на сервер
        sendResultsToServer(score)

        // Переходим к фрагменту с результатами
        navigateToTestResultFragment(score, results)
    }

    private fun calculateScore(): Int {
        var score = 0
        for ((questionId, selectedAnswer) in selectedAnswers) {
            val correctAnswer = questions.find { it.id == questionId }?.answers?.find { it.is_correct }
            if (selectedAnswer.id == correctAnswer?.id) {
                score++
            }
        }
        return score
    }

    private fun sendResultsToServer(score: Int) {
        val userId = user.id ?: throw IllegalStateException("User ID is null")

        val testResult = TestResult(
            user_id = userId,
            test_id = test.id,
            score = score,
            started_at =  testStartTime
        )

        lifecycleScope.launch {
            try {
                Log.d("API", "Отправка результатов теста: $testResult")
                val response = ApiClient.apiService.submitTestResult(testResult)
                if (response.isSuccessful) {
                    Log.d("API", "Результаты успешно отправлены: ${response.body()}")
                    Toast.makeText(context, "Результаты отправлены!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("API", "Ошибка отправки: ${response.code()}, ${response.errorBody()?.string()}")
                    Toast.makeText(
                        context,
                        "Ошибка отправки: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("API", "Ошибка при отправке результатов: ${e.message}", e)
                handleException(e)
            }
        }
    }

    private fun handleException(e: Exception) {
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

    private fun navigateToTestResultFragment(score: Int, results: List<ResultItem>) {
        val bundle = Bundle().apply {
            putInt("score", score)
            putInt("totalQuestions", questions.size)
            putParcelableArrayList("results", ArrayList(results))
            putString("testTitle","Тема ${test.id}. "+test.title)
        }

        val testResultFragment = TestResultFragment().apply {
            arguments = bundle
        }

        (requireActivity() as SecondActivityWithBottomNavMenu).replaceFragment(testResultFragment, bundle)
    }
    private fun updateNavigationButtonsState() {
        val btnNext = view?.findViewById<Button>(R.id.btnNext)
        val btnBack = view?.findViewById<Button>(R.id.btnBack)

        // Кнопка "Далее"
        btnNext?.isEnabled = answersAdapter.getSelectedAnswer() != null

        // Кнопка "Назад"
        btnBack?.isEnabled = currentQuestionIndex > 0
    }

    private fun handleBackButtonClick() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
            updateQuestion()
        }
    }

    private fun handleNextButtonClick() {
        if (currentQuestionIndex < questions.size) {
            currentQuestionIndex++
            updateQuestion()
        } else {
            finishTest()
        }
    }
    override fun onPause() {
        super.onPause()
        tvUpperCenter.text=""
        tvUpperCenter.visibility=View.VISIBLE
        tvUpperLeftCorner.visibility=View.GONE

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
    }

    override fun onResume() {
        super.onResume()
        tvUpperCenter.text=test.title
        tvUpperCenter.visibility=View.VISIBLE
        tvUpperLeftCorner.visibility=View.GONE

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)

    }
    override fun onDestroy() {
        super.onDestroy()
        tvUpperCenter.text=""
        tvUpperCenter.visibility=View.VISIBLE
        tvUpperLeftCorner.visibility=View.GONE

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
    }
}