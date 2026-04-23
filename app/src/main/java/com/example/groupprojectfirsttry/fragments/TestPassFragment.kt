package com.example.groupprojectfirsttry.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TestAnswerRequest
import com.example.groupprojectfirsttry.api.TestResult
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.Answer
import com.example.groupprojectfirsttry.simpleClasses.Question
import com.example.groupprojectfirsttry.simpleClasses.ResultItem
import com.example.groupprojectfirsttry.simpleClasses.Test
import com.example.groupprojectfirsttry.simpleClasses.User
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

class TestPassFragment : Fragment(R.layout.fragment_test_pass) {

    private lateinit var test: Test
    private var questions = emptyList<Question>()
    private val selectedAnswers = mutableMapOf<Int, Answer>() // questionId -> chosen answer
    
    private lateinit var llQuestionsContainer: LinearLayout
    private lateinit var btnSubmitTest: MaterialButton
    private lateinit var btnRetryTest: MaterialButton
    private lateinit var cvResultBanner: CardView
    private lateinit var tvResultStatus: TextView
    private lateinit var tvResultScore: TextView

    private lateinit var userProvider: UserProvider
    private lateinit var user: User
    private lateinit var testStartTime: String
    
    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = requireArguments().getParcelable("user") ?: throw IllegalArgumentException("User not found")
        
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback!!)
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

        llQuestionsContainer = view.findViewById(R.id.llQuestionsContainer)
        btnSubmitTest = view.findViewById(R.id.btnSubmitTest)
        btnRetryTest = view.findViewById(R.id.btnRetryTest)
        cvResultBanner = view.findViewById(R.id.cvResultBanner)
        tvResultStatus = view.findViewById(R.id.tvResultStatus)
        tvResultScore = view.findViewById(R.id.tvResultScore)

        btnSubmitTest.setOnClickListener {
            if (validateAllAnswered()) {
                finishTest()
            } else {
                Toast.makeText(context, "Пожалуйста, ответьте на все вопросы", Toast.LENGTH_SHORT).show()
            }
        }

        btnRetryTest.setOnClickListener {
            restartTest()
        }

        loadQuestions(test.id)
    }

    private fun loadQuestions(testId: Int) = lifecycleScope.launch {
        try {
            val response = ApiClient.apiService.getQuestions(testId)
            if (response.isNotEmpty()) {
                questions = response
                renderQuestions()
            } else {
                Toast.makeText(context, "Нет вопросов для этого теста", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun renderQuestions() {
        if (!isAdded) return
        llQuestionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        questions.forEachIndexed { index, question ->
            val questionView = inflater.inflate(R.layout.item_test_question, llQuestionsContainer, false)
            
            questionView.findViewById<TextView>(R.id.tvQuestionNumber).text = "Вопрос ${index + 1}"
            questionView.findViewById<TextView>(R.id.tvQuestionText).text = question.text
            
            val rgAnswers = questionView.findViewById<RadioGroup>(R.id.rgAnswers)
            
            question.answers.forEach { answer ->
                val rb = RadioButton(requireContext()).apply {
                    text = answer.text
                    id = View.generateViewId()
                    tag = answer
                    textSize = 16f
                    setPadding(0, 12, 0, 12)
                    buttonTintList = ColorStateList.valueOf(Color.parseColor("#8E8E93"))
                    
                    val params = RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.MATCH_PARENT,
                        RadioGroup.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 8, 0, 8)
                    layoutParams = params
                    
                    setBackgroundResource(R.drawable.bg_answer_item_selector)
                }
                rgAnswers.addView(rb)
            }

            rgAnswers.setOnCheckedChangeListener { group, checkedId ->
                val checkedRb = group.findViewById<RadioButton>(checkedId)
                if (checkedRb != null) {
                    val selectedAnswer = checkedRb.tag as Answer
                    selectedAnswers[question.id] = selectedAnswer
                    updateSubmitButtonState()
                }
            }

            llQuestionsContainer.addView(questionView)
        }
        updateSubmitButtonState()
    }

    private fun updateSubmitButtonState() {
        val isAllAnswered = selectedAnswers.size == questions.size
        btnSubmitTest.isEnabled = isAllAnswered
        btnSubmitTest.backgroundTintList = ColorStateList.valueOf(
            if (isAllAnswered) Color.parseColor("#000000") else Color.parseColor("#8E8E93")
        )
    }

    private fun validateAllAnswered(): Boolean {
        return selectedAnswers.size == questions.size
    }

    private fun finishTest() {
        val score = calculateScore()
        val total = questions.size
        
        // Показываем плашку результата
        cvResultBanner.visibility = View.VISIBLE
        tvResultScore.text = "Правильных ответов: $score из $total"
        
        if (score == total) {
            tvResultStatus.text = "✅ Отличный результат!"
            cvResultBanner.setCardBackgroundColor(Color.parseColor("#F1FFF1"))
        } else {
            tvResultStatus.text = "📚 Попробуйте ещё раз"
            cvResultBanner.setCardBackgroundColor(Color.parseColor("#FFF1F1"))
        }

        // Меняем кнопки
        btnSubmitTest.visibility = View.GONE
        btnRetryTest.visibility = View.VISIBLE

        // Блокируем выбор ответов
        disableRadioGroups()

        // Отправка результатов
        sendResultsToServer(score)
    }

    private fun disableRadioGroups() {
        for (i in 0 until llQuestionsContainer.childCount) {
            val qView = llQuestionsContainer.getChildAt(i)
            val rg = qView.findViewById<RadioGroup>(R.id.rgAnswers)
            for (j in 0 until rg.childCount) {
                rg.getChildAt(j).isEnabled = false
            }
        }
    }

    private fun restartTest() {
        selectedAnswers.clear()
        cvResultBanner.visibility = View.GONE
        btnRetryTest.visibility = View.GONE
        btnSubmitTest.visibility = View.VISIBLE
        renderQuestions()
    }

    private fun calculateScore(): Int {
        var score = 0
        questions.forEach { question ->
            val selected = selectedAnswers[question.id]
            val correct = question.answers.find { it.is_correct }
            if (selected?.id == correct?.id) {
                score++
            }
        }
        return score
    }

    private fun sendResultsToServer(score: Int) {
        val userId = user.id ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val completedAt = sdf.format(Date())
        val finalPercentage = (score * 100) / questions.size

        val answersRequests = questions.map { question ->
            val selected = selectedAnswers[question.id]
            val correct = question.answers.find { it.is_correct }
            TestAnswerRequest(
                question_id = question.id,
                chosen_answer_id = selected?.id,
                is_correct = selected?.id == correct?.id
            )
        }

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
                ApiClient.apiService.submitTestResult(testResult)
            } catch (e: Exception) {
                Log.e("TestPass", "Error sending results", e)
            }
        }
    }

    fun showExitConfirmationDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Выйти?")
            .setMessage("Ваш прогресс теста будет потерян.")
            .setPositiveButton("Да") { _, _ ->
                backPressedCallback?.isEnabled = false
                parentFragmentManager.popBackStack()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun handleException(e: Exception) {
        Log.e("TestPass", "Error", e)
        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        if (isAdded) {
            requireActivity().findViewById<TextView>(R.id.textViewUpper).text = ""
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            requireActivity().findViewById<TextView>(R.id.textViewUpper).text = test.title
        }
    }
}
