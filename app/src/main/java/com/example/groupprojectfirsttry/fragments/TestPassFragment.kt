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
import com.example.groupprojectfirsttry.api.TestAnswerRequest
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

    private lateinit var userProvider: UserProvider
    private lateinit var user: User
    private lateinit var testStartTime: String // Время начала теста

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = requireArguments().getParcelable("user") ?: throw IllegalArgumentException("User not found")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UserProvider) {
            userProvider = context
            user = userProvider.getUser()
        } else {
            throw RuntimeException("$context must implement UserProvider")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        test = requireArguments().getParcelable("test") ?: throw IllegalArgumentException("Test not found")
        
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        testStartTime = sdf.format(Date())

        tvUpperLeftCorner = requireActivity().findViewById(R.id.textViewLeftUpperCorner)
        tvUpperCenter = requireActivity().findViewById(R.id.textViewUpper)
        ivTestLogo = requireActivity().findViewById(R.id.imageViewTestLogo)
        clUpHead = requireActivity().findViewById(R.id.constraintLayoutUpHead)
        bnmDown = requireActivity().findViewById(R.id.bottom_nav)

        val answersList = view.findViewById<RecyclerView>(R.id.answersList)
        answersList.layoutManager = LinearLayoutManager(context)

        val btnNext = view.findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener {
            handleNextButtonClick()
        }

        val btnBack = view.findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            handleBackButtonClick()
        }

        loadQuestions(test.id)
    }

    private fun loadQuestions(testId: Int) = lifecycleScope.launch {
        try {
            Log.d("API", "Запрос вопросов для теста: $testId")
            val response = ApiClient.apiService.getQuestions(testId)
            if (response.isNotEmpty()) {
                questions = response
                updateQuestion()
            } else {
                Toast.makeText(context, "Нет вопросов для этого теста", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun updateQuestion() {
        if (currentQuestionIndex >= questions.size) return

        val question = questions[currentQuestionIndex]
        view?.findViewById<TextView>(R.id.tvQuestion)?.text = question.text

        answersAdapter = AnswersAdapter(
            question.answers,
            onAnswerSelected = { answer ->
                selectedAnswers[question.id] = answer
                updateNextButtonState()
            }
        )
        view?.findViewById<RecyclerView>(R.id.answersList)?.adapter = answersAdapter

        val tvArrowTest = view?.findViewById<TextView>(R.id.textViewArrowTest)
        tvArrowTest?.text = if (currentQuestionIndex == questions.lastIndex) {
            "Завершить\nтест"
        } else {
            "Следующий\nвопрос"
        }

        updateNavigationButtonsState()
    }

    private fun updateNextButtonState() {
        val btnNext = view?.findViewById<Button>(R.id.btnNext)
        btnNext?.isEnabled = answersAdapter.getSelectedAnswer() != null
    }

    private fun finishTest() {
        if (questions.isEmpty()) {
            Toast.makeText(context, "Тест пуст", Toast.LENGTH_SHORT).show()
            return
        }

        for (question in questions) {
            if (selectedAnswers[question.id] == null) {
                Toast.makeText(context, "Выберите ответ для всех вопросов", Toast.LENGTH_SHORT).show()
                currentQuestionIndex = questions.indexOf(question)
                updateQuestion()
                return
            }
        }

        val score = calculateScore()
        val results = mutableListOf<ResultItem>()
        val answersRequests = mutableListOf<TestAnswerRequest>()

        for (question in questions) {
            val selectedAnswer = selectedAnswers[question.id]
            val correctAnswer = question.answers.find { it.is_correct }
            val isCorrect = selectedAnswer?.id == correctAnswer?.id
            
            results.add(ResultItem(
                questionText = question.text,
                answers = question.answers,
                selectedAnswerText = selectedAnswer?.text ?: "Не выбран",
                isCorrect = isCorrect
            ))

            answersRequests.add(TestAnswerRequest(
                question_id = question.id,
                chosen_answer_id = selectedAnswer?.id
            ))
        }

        sendResultsToServer(score, results, answersRequests)
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

    private fun sendResultsToServer(score: Int, results: List<ResultItem>, answersRequests: List<TestAnswerRequest>) {
        val userId = user.id ?: throw IllegalStateException("User ID is null")

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val completedAt = sdf.format(Date())
        
        val finalPercentage = if (questions.isNotEmpty()) (score * 100) / questions.size else 0

        val testResult = TestResult(
            user_id = userId,
            test_id = test.id,
            score = finalPercentage,
            started_at = testStartTime,
            completed_at = completedAt,
            answers = answersRequests
        )

        lifecycleScope.launch {
            try {
                Log.d("API", "Отправка результатов теста: $testResult")
                val response = ApiClient.apiService.submitTestResult(testResult)
                if (response.isSuccessful) {
                    val resultResponse = response.body()
                    Log.d("API", "Результаты успешно отправлены! ID из тела: ${resultResponse?.id}")
                    
                    // Дополнительная проверка на случай если ID пришел в SubmitResponse стиле
                    val finalId = resultResponse?.id
                    
                    if (finalId != null) {
                        Toast.makeText(context, "Результаты успешно отправлены!", Toast.LENGTH_SHORT).show()
                        navigateToTestResultFragment(score, results, finalId)
                    } else {
                        Log.e("API", "ID результата не получен от сервера!")
                        Toast.makeText(context, "Ошибка: сервер не вернул ID", Toast.LENGTH_SHORT).show()
                        navigateToTestResultFragment(score, results, null)
                    }
                } else {
                    Log.e("API", "Ошибка отправки: ${response.code()} ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Ошибка отправки: ${response.code()}", Toast.LENGTH_SHORT).show()
                    navigateToTestResultFragment(score, results, null)
                }
            } catch (e: Exception) {
                Log.e("API", "Ошибка при отправке: ${e.message}", e)
                Toast.makeText(context, "Ошибка соединения", Toast.LENGTH_SHORT).show()
                navigateToTestResultFragment(score, results, null)
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

    private fun navigateToTestResultFragment(score: Int, results: List<ResultItem>, resultId: String?) {
        val bundle = Bundle().apply {
            putInt("score", score)
            putInt("totalQuestions", questions.size)
            putParcelableArrayList("results", ArrayList(results))
            putString("testTitle", "Тест: ${test.title}")
            putString("resultId", resultId)
        }

        val testResultFragment = TestResultFragment().apply {
            arguments = bundle
        }

        (requireActivity() as? SecondActivityWithBottomNavMenu)?.replaceFragment(testResultFragment, bundle)
    }

    private fun updateNavigationButtonsState() {
        val btnNext = view?.findViewById<Button>(R.id.btnNext)
        val btnBack = view?.findViewById<Button>(R.id.btnBack)
        btnNext?.isEnabled = answersAdapter.getSelectedAnswer() != null
        btnBack?.isEnabled = currentQuestionIndex > 0
    }

    private fun handleBackButtonClick() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
            updateQuestion()
        }
    }

    private fun handleNextButtonClick() {
        if (currentQuestionIndex < questions.size - 1) {
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
        clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
    }

    override fun onResume() {
        super.onResume()
        tvUpperCenter.text=test.title
        tvUpperCenter.visibility=View.VISIBLE
        tvUpperLeftCorner.visibility=View.GONE
        clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_gray_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_gray_background, context?.theme)
    }
}