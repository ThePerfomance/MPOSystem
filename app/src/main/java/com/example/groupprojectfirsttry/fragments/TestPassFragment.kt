package com.example.groupprojectfirsttry.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.api.*
import com.example.groupprojectfirsttry.simpleClasses.*
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TestPassFragment : Fragment(R.layout.fragment_test_pass) {

    private lateinit var test: Test
    private var questions: List<Question> = emptyList()
    private val selectedAnswers = mutableMapOf<Int, Answer>() // questionId -> chosen answer
    private var currentResultId: String? = null

    private lateinit var llQuestionsContainer: LinearLayout
    private lateinit var btnSubmitTest: MaterialButton
    private lateinit var btnRetryTest: MaterialButton
    private lateinit var cvResultBanner: CardView
    private lateinit var tvResultStatus: TextView
    private lateinit var tvResultScore: TextView
    
    // Header views
    private lateinit var tvTitleHeader: TextView
    private lateinit var tvProgressHeader: TextView
    private lateinit var pbHeader: ProgressBar
    private lateinit var btnBackHeader: View

    private lateinit var shimmerTestPass: ShimmerFrameLayout
    private lateinit var llTestMainContent: View

    private lateinit var userProvider: UserProvider
    private lateinit var user: User
    private lateinit var testStartTime: String

    var isFinished = false
        private set
    
    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = requireArguments().getParcelable("user") ?: throw IllegalArgumentException("User not found")
        
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFinished) {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                } else {
                    showExitConfirmationDialog()
                }
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

        initViews(view)
        setupListeners()

        startTestSession(test.id)
    }

    private fun initViews(view: View) {
        llQuestionsContainer = view.findViewById(R.id.llQuestionsContainer)
        btnSubmitTest = view.findViewById(R.id.btnSubmitTest)
        btnRetryTest = view.findViewById(R.id.btnRetryTest)
        cvResultBanner = view.findViewById(R.id.cvResultBanner)
        tvResultStatus = view.findViewById(R.id.tvResultStatus)
        tvResultScore = view.findViewById(R.id.tvResultScore)
        
        tvTitleHeader = view.findViewById(R.id.tvTestTitleHeader)
        tvProgressHeader = view.findViewById(R.id.tvTestProgressHeader)
        pbHeader = view.findViewById(R.id.pbTestHeader)
        btnBackHeader = view.findViewById(R.id.btnBackTest)

        shimmerTestPass = view.findViewById(R.id.shimmer_test_pass)
        llTestMainContent = view.findViewById(R.id.llTestMainContent)

        tvTitleHeader.text = test.title
        updateProgressHeader(0)

        // Отключаем обрезку детей, чтобы анимация масштабирования не обрезалась
        llQuestionsContainer.clipChildren = false
        llQuestionsContainer.clipToPadding = false
        (view as? ViewGroup)?.clipChildren = false
        (view as? ViewGroup)?.clipToPadding = false
    }

    private fun setupListeners() {
        btnSubmitTest.setOnClickListener {
            if (selectedAnswers.size == questions.size) {
                finishTest()
            } else {
                Toast.makeText(context, "Пожалуйста, ответьте на все вопросы", Toast.LENGTH_SHORT).show()
            }
        }

        btnRetryTest.setOnClickListener {
            restartTest()
        }

        btnBackHeader.setOnClickListener {
            if (isFinished) {
                parentFragmentManager.popBackStack()
            } else {
                showExitConfirmationDialog()
            }
        }
    }

    private fun startLoading() {
        shimmerTestPass.visibility = View.VISIBLE
        shimmerTestPass.startShimmer()
        llTestMainContent.visibility = View.GONE
    }

    private fun stopLoading() {
        shimmerTestPass.stopShimmer()
        shimmerTestPass.visibility = View.GONE
        llTestMainContent.visibility = View.VISIBLE
    }

    private fun updateProgressHeader(answeredCount: Int) {
        val total = if (questions.isEmpty()) 1 else questions.size
        tvProgressHeader.text = "$answeredCount / $total"
        
        // Плавная анимация прогресс-бара
        pbHeader.max = total * 100
        val targetProgress = answeredCount * 100

        android.animation.ObjectAnimator.ofInt(pbHeader, "progress", targetProgress)
            .setDuration(500)
            .apply {
                interpolator = android.view.animation.DecelerateInterpolator()
                start()
            }
    }

    private fun startTestSession(testId: Int) = lifecycleScope.launch {
        startLoading()
        try {
            val body = mapOf("user_id" to user.id)
            val response = ApiClient.apiService.startTest(testId, body)
            
            if (response.isSuccessful && response.body() != null) {
                val startData = response.body()!!
                currentResultId = startData.resultId
                questions = startData.test?.questions ?: emptyList()
                
                updateProgressHeader(0)
                renderQuestions()
            } else {
                Toast.makeText(context, "Ошибка при запуске теста", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("TestPass", "Error starting test session", e)
            Toast.makeText(context, "Ошибка сети", Toast.LENGTH_SHORT).show()
        } finally {
            if (isAdded) stopLoading()
        }
    }

    private fun renderQuestions() {
        if (!isAdded) return
        llQuestionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        if (questions.isEmpty()) {
            Toast.makeText(context, "Нет вопросов для этого теста", Toast.LENGTH_SHORT).show()
            return
        }

        questions.forEachIndexed { index, question ->
            val questionView = inflater.inflate(R.layout.item_test_question, llQuestionsContainer, false)
            
            questionView.findViewById<TextView>(R.id.tvQuestionNumber).text = "Вопрос ${index + 1}"
            questionView.findViewById<TextView>(R.id.tvQuestionText).text = question.text
            
            val rgAnswers = questionView.findViewById<RadioGroup>(R.id.rgAnswers)
            rgAnswers.clipChildren = false
            rgAnswers.clipToPadding = false
            
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
                    // Добавляем горизонтальные отступы (12dp), чтобы при увеличении (scale)
                    // края не выходили за границы родителя
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
                    checkedRb.animate()
                        .scaleX(1.03f)
                        .scaleY(1.03f)
                        .setDuration(100)
                        .withEndAction {
                            checkedRb.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }
                        .start()

                    val selectedAnswer = checkedRb.tag as Answer
                    selectedAnswers[question.id] = selectedAnswer
                    updateSubmitButtonState()
                    updateProgressHeader(selectedAnswers.size)
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
            if (isAllAnswered) Color.parseColor("#0A0B0E") else Color.parseColor("#8E8E93")
        )
    }

    private fun finishTest() {
        isFinished = true
        // Больше не считаем score здесь локально!

        btnSubmitTest.visibility = View.GONE
        disableRadioGroups()

        // Сначала отправляем на сервер, а UI обновим когда придет ответ
        sendResultsToServer()
    }

    private fun disableRadioGroups() {
        for (i in 0 until llQuestionsContainer.childCount) {
            val rg = llQuestionsContainer.getChildAt(i).findViewById<RadioGroup>(R.id.rgAnswers)
            for (j in 0 until rg.childCount) {
                rg.getChildAt(j).isEnabled = false
            }
        }
    }

    private fun restartTest() {
        isFinished = false
        selectedAnswers.clear()
        cvResultBanner.visibility = View.GONE
        btnRetryTest.visibility = View.GONE
        btnSubmitTest.visibility = View.VISIBLE
        updateProgressHeader(0)
        startTestSession(test.id)
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

    private fun sendResultsToServer() {
        val resultId = currentResultId ?: return

        val answers = questions.map { question ->
            UserAnswerInput(
                question = question.id,
                answer = selectedAnswers[question.id]?.id ?: 0
            )
        }

        val request = SubmitTestRequest(answers = answers)

        // Показываем загрузку, пока сервер считает результаты
        startLoading()

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.submitTest(resultId, request)
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!

                    val earned = result.earnedPoints ?: 0
                    val total = result.totalPoints ?: questions.size

                    // Обновляем UI результатами от СЕРВЕРА
                    cvResultBanner.visibility = View.VISIBLE
                    tvResultScore.text = "Правильных ответов: $earned из $total"

                    if (earned == total && total > 0) {
                        tvResultStatus.text = "✅ Отличный результат!"
                        cvResultBanner.setCardBackgroundColor(Color.parseColor("#F1FFF1"))
                    } else {
                        tvResultStatus.text = "📚 Попробуйте ещё раз"
                        cvResultBanner.setCardBackgroundColor(Color.parseColor("#FFF1F1"))

                        // Создаем тренировочную сессию для ошибок (если нужно)
                        ApiClient.apiService.createTrainingSession(resultId)
                    }

                    btnRetryTest.visibility = View.VISIBLE

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("TestPass", "Server error: $errorBody")
                    Toast.makeText(context, "Ошибка сохранения результатов", Toast.LENGTH_SHORT).show()
                    btnRetryTest.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("TestPass", "Error submitting test", e)
                Toast.makeText(context, "Ошибка сети при отправке", Toast.LENGTH_SHORT).show()
                btnRetryTest.visibility = View.VISIBLE
            } finally {
                if (isAdded) stopLoading()
            }
        }
    }

    fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выйти из теста?")
            .setMessage("Ваш прогресс будет потерян. Вы уверены?")
            .setPositiveButton("Да") { _, _ ->
                backPressedCallback?.isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .setNegativeButton("Нет", null)
            .show()
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
