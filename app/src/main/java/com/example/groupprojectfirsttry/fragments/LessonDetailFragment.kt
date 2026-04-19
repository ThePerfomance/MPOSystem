package com.example.groupprojectfirsttry.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.adapters.AnswersAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TestAnswerRequest
import com.example.groupprojectfirsttry.api.TestResult
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.Answer
import com.example.groupprojectfirsttry.simpleClasses.Lesson
import com.example.groupprojectfirsttry.simpleClasses.Question
import com.example.groupprojectfirsttry.simpleClasses.ResultItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LessonDetailFragment : Fragment(R.layout.fragment_lesson_detail) {

    private var webView: WebView? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var fullscreenContainer: android.widget.FrameLayout? = null
    private var currentTab = 0

    private var questions: List<Question> = emptyList()
    private var currentQuestionIndex = 0
    private val selectedAnswers = mutableMapOf<Int, Answer>()
    private lateinit var answersAdapter: AnswersAdapter
    private var testStartTime: String = ""
    
    var isTestActive = false
        private set
    
    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTestActive) {
                    showExitConfirmationDialog()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fullscreenContainer = view.findViewById(R.id.fullscreenContainer)
        val lesson = arguments?.getParcelable<Lesson>("lesson")
        
        // Логируем объект урока в формате JSON
        Log.d("VideoDebug", "--- Lesson Data Received ---")
        Log.d("VideoDebug", "Lesson JSON: ${Gson().toJson(lesson)}")

        val blockTitle = arguments?.getString("block_title") ?: "Блок"

        val lessonTitle = lesson?.title ?: "Урок"
        view.findViewById<TextView>(R.id.tvLessonTitleDetail).text = lessonTitle
        view.findViewById<TextView>(R.id.tvBlockTitleDetail).text = blockTitle

        val tvSummaryContent = view.findViewById<TextView>(R.id.tvSummaryContent)
        tvSummaryContent.text = lesson?.summary ?: "Нет описания"

        val tvDuration = view.findViewById<TextView>(R.id.tvVideoDurationDetail)
        val minutes = (lesson?.duration ?: 0) / 60
        tvDuration.text = "Продолжительность: $minutes мин"

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            if (isTestActive) {
                showExitConfirmationDialog()
            } else {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        // Исправлено: передаем lesson?.video?.link, так как video теперь объект
        setupWebView(view, lesson?.video?.link)
        setupTabs(view, lesson)
        setupTestNavigation(view)

        view.findViewById<Button>(R.id.btnStartTest).setOnClickListener {
            startTestSession()
        }

        if (lesson?.test != null) {
            loadQuestions(lesson.test)
        } else {
            view.findViewById<View>(R.id.llStartTestContainer).visibility = View.GONE
            view.findViewById<View>(R.id.llTestContainer).visibility = View.GONE
            view.findViewById<View>(R.id.tvNoTest).visibility = View.VISIBLE
        }
    }

    private fun startTestSession() {
        val view = view ?: return
        isTestActive = true
        
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        testStartTime = sdf.format(Date())
        
        view.findViewById<View>(R.id.llStartTestContainer).visibility = View.GONE
        view.findViewById<View>(R.id.llTestContainer).visibility = View.VISIBLE

        if (questions.isNotEmpty()) {
            updateQuestion()
        } else {
            Toast.makeText(context, "Загрузка вопросов...", Toast.LENGTH_SHORT).show()
        }
    }

    fun showExitConfirmationDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Завершить попытку?")
            .setMessage("Вы уверены, что хотите выйти из теста? Ваш текущий прогресс будет сохранен.")
            .setPositiveButton("Да, выйти") { _, _ ->
                isTestActive = false
                backPressedCallback?.isEnabled = false
                submitTest(isForcedExit = true)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setupWebView(view: View, videoLink: String?) {
        val trimmedLink = videoLink?.trim() ?: ""
        Log.d("VideoDebug", "--- setupWebView start ---")
        Log.d("VideoDebug", "Original link string: '$trimmedLink'")
        
        webView = view.findViewById(R.id.webViewRutube)
        if (trimmedLink.isEmpty()) {
            Log.e("VideoDebug", "Empty video link")
            return
        }

        val llHeader = view.findViewById<View>(R.id.llHeader)
        val llTabs = view.findViewById<View>(R.id.llTabs)

        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
            
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return true
                    return handleUrl(url)
                }
                
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return handleUrl(url ?: "")
                }

                private fun handleUrl(url: String): Boolean {
                    Log.d("VideoDebug", "WebView handleUrl: $url")
                    val allowedDomains = listOf("rutube.ru", "vkvideo.ru", "vk.com", "rtbcdn.ru", "vk.me", "yastatic.net")
                    if (allowedDomains.any { url.contains(it) } || url.startsWith("data:")) {
                        return false 
                    }
                    Log.w("VideoDebug", "Blocked navigation to: $url")
                    return true
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d("VideoDebug", "onPageFinished: $url")
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    Log.e("VideoDebug", "WebView Error: [${error?.errorCode}] ${error?.description} for ${request?.url}")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (customView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    customView = view
                    fullscreenContainer?.addView(customView)
                    fullscreenContainer?.visibility = View.VISIBLE
                    customViewCallback = callback
                    llHeader.visibility = View.GONE
                    llTabs.visibility = View.GONE
                    activity?.findViewById<View>(R.id.bottom_nav)?.visibility = View.GONE
                    activity?.findViewById<View>(R.id.constraintLayoutUpHead)?.visibility = View.GONE
                    activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }

                override fun onHideCustomView() {
                    if (customView == null) return
                    fullscreenContainer?.visibility = View.GONE
                    fullscreenContainer?.removeView(customView)
                    customView = null
                    customViewCallback?.onCustomViewHidden()
                    llHeader.visibility = View.VISIBLE
                    llTabs.visibility = View.VISIBLE
                    activity?.findViewById<View>(R.id.bottom_nav)?.visibility = View.VISIBLE
                    activity?.findViewById<View>(R.id.constraintLayoutUpHead)?.visibility = View.VISIBLE
                    activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            val embedUrl = getVideoEmbedUrl(trimmedLink)
            Log.d("VideoDebug", "Calculated embedUrl: $embedUrl")
            
            val isVk = embedUrl.contains("vk.com", ignoreCase = true) || embedUrl.contains("vkvideo.ru", ignoreCase = true)
            val baseUrl = if (isVk) "https://vkvideo.ru" else "https://rutube.ru"
            Log.d("VideoDebug", "Using baseUrl: $baseUrl")

            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body, html { margin: 0; padding: 0; height: 100%; width: 100%; background: #000; overflow: hidden; }
                        iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none; }
                    </style>
                </head>
                <body>
                    <iframe src="$embedUrl" 
                        allow="autoplay; encrypted-media; fullscreen; picture-in-picture; screen-wake-lock;" 
                        allowfullscreen frameborder="0">
                    </iframe>
                </body>
                </html>
            """.trimIndent()
            
            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
    }

    private fun getVideoEmbedUrl(url: String): String {
        var trimmed = url.trim()
        
        // 1. Проверка на UUID (8-4-4-4-12 символов с дефисами). Если это UUID, считаем его RuTube ID.
        val uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        if (uuidPattern.matches(trimmed)) {
            val cleanId = trimmed.replace("-", "")
            Log.d("VideoDebug", "Detected UUID, converting to RuTube embed: $cleanId")
            return "https://rutube.ru/play/embed/$cleanId/"
        }

        // 2. Извлекаем src из iframe, если передан тег
        if (trimmed.contains("<iframe", ignoreCase = true)) {
            val srcMatch = "src\\s*=\\s*['\"]([^'\"]+)['\"]".toRegex(RegexOption.IGNORE_CASE).find(trimmed)
            if (srcMatch != null) {
                trimmed = srcMatch.groupValues[1].replace("&amp;", "&")
                Log.d("VideoDebug", "Extracted URL from iframe: $trimmed")
            }
        }

        val isVkLink = trimmed.contains("vk.com", ignoreCase = true) || trimmed.contains("vkvideo.ru", ignoreCase = true)
        val isRutubeLink = trimmed.contains("rutube.ru", ignoreCase = true)

        return when {
            isVkLink -> {
                if (trimmed.contains("video_ext.php")) return trimmed
                val match = "video(-?\\d+)_(\\d+)".toRegex().find(trimmed)
                if (match != null) {
                    val oid = match.groupValues[1]
                    val id = match.groupValues[2]
                    val hash = if (trimmed.contains("hash=")) {
                        trimmed.substringAfter("hash=").substringBefore("&").substringBefore("/")
                    } else null
                    val hashParam = if (hash != null) "&hash=$hash" else ""
                    "https://vkvideo.ru/video_ext.php?oid=$oid&id=$id&hd=2$hashParam"
                } else trimmed
            }
            isRutubeLink -> {
                if (trimmed.contains("play/embed")) return trimmed
                val match = "(?:video|embed)/(?:private/)?([a-f0-9]{32})".toRegex(RegexOption.IGNORE_CASE).find(trimmed)
                if (match != null) {
                    val id = match.groupValues[1]
                    val p = if (trimmed.contains("p=")) {
                        val pVal = trimmed.substringAfter("p=").substringBefore("&").substringBefore("/")
                        "?p=$pVal"
                    } else ""
                    "https://rutube.ru/play/embed/$id/$p"
                } else {
                    val fallbackMatch = "([a-f0-9]{32})".toRegex(RegexOption.IGNORE_CASE).find(trimmed)
                    if (fallbackMatch != null) {
                        val id = fallbackMatch.groupValues[1]
                        "https://rutube.ru/play/embed/$id/"
                    } else trimmed
                }
            }
            else -> trimmed
        }
    }

    private fun setupTabs(view: View, lesson: Lesson?) {
        val tabVideo = view.findViewById<LinearLayout>(R.id.tabVideo)
        val tabSummary = view.findViewById<LinearLayout>(R.id.tabSummary)
        val tabTest = view.findViewById<LinearLayout>(R.id.tabTest)

        val contentVideo = view.findViewById<View>(R.id.cvVideoContent)
        val contentSummary = view.findViewById<View>(R.id.nsvSummaryContent)
        val contentTest = view.findViewById<View>(R.id.clTestContent)

        // Исправлено: video теперь объект, проверяем link
        val hasVideo = !lesson?.video?.link.isNullOrEmpty()
        tabVideo.visibility = if (hasVideo) View.VISIBLE else View.GONE
        currentTab = if (hasVideo) 0 else 1

        val tabs = listOf(tabVideo, tabSummary, tabTest)
        val contents = listOf(contentVideo, contentSummary, contentTest)

        tabs.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                if (currentTab == 2 && isTestActive && index != 2) {
                    showExitConfirmationDialog()
                } else {
                    updateTabs(index, tabs, contents)
                }
            }
        }
        updateTabs(currentTab, tabs, contents)
    }

    private fun updateTabs(selectedIndex: Int, tabs: List<LinearLayout>, contents: List<View>) {
        currentTab = selectedIndex
        tabs.forEachIndexed { index, layout ->
            val isSelected = index == selectedIndex
            if (index == 2 && isSelected) {
                layout.setBackgroundResource(R.drawable.bg_test_tab_selected)
            } else {
                layout.setBackgroundResource(if (isSelected) R.drawable.bg_tab_selected else R.drawable.bg_gray_tag)
            }

            val icon = layout.getChildAt(0) as? android.widget.ImageView
            val text = layout.getChildAt(1) as? TextView
            
            val color = if (isSelected) {
                if (index == 2) android.graphics.Color.parseColor("#FF0000")
                else resources.getColor(R.color.OnboardingPrimaryTextColor, null)
            } else {
                resources.getColor(R.color.OnboardingSecondaryTextColor, null)
            }
            
            icon?.setColorFilter(color)
            text?.setTextColor(color)
            text?.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        contents.forEachIndexed { index, contentView ->
            contentView.visibility = if (index == selectedIndex) View.VISIBLE else View.GONE
        }
        if (selectedIndex != 0) {
            webView?.onPause()
            webView?.pauseTimers()
        } else {
            webView?.onResume()
            webView?.resumeTimers()
        }
    }

    private fun loadQuestions(testId: Int) = viewLifecycleOwner.lifecycleScope.launch {
        val pb = view?.findViewById<ProgressBar>(R.id.pbTestLoading)
        pb?.visibility = View.VISIBLE
        try {
            val response = ApiClient.apiService.getQuestions(testId)
            if (response.isNotEmpty()) {
                questions = response
            } else {
                view?.findViewById<View>(R.id.llStartTestContainer)?.visibility = View.GONE
                view?.findViewById<View>(R.id.llTestContainer)?.visibility = View.GONE
                view?.findViewById<View>(R.id.tvNoTest)?.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e("LessonDetail", "Error loading questions", e)
            Toast.makeText(context, "Ошибка загрузки теста", Toast.LENGTH_SHORT).show()
        } finally {
            pb?.visibility = View.GONE
        }
    }

    private fun updateQuestion() {
        val view = view ?: return
        if (currentQuestionIndex >= questions.size) return

        val question = questions[currentQuestionIndex]
        view.findViewById<TextView>(R.id.tvQuestion).text = question.text
        view.findViewById<TextView>(R.id.tvTestProgress).text = "Вопрос ${currentQuestionIndex + 1} из ${questions.size}"

        answersAdapter = AnswersAdapter(question.answers, selectedAnswers[question.id]) { answer ->
            selectedAnswers[question.id] = answer
            view.findViewById<View>(R.id.btnTestNext).isEnabled = true
        }

        val rv = view.findViewById<RecyclerView>(R.id.rvAnswers)
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = answersAdapter

        val btnNext = view.findViewById<View>(R.id.btnTestNext)
        btnNext.isEnabled = selectedAnswers.containsKey(question.id)
    }

    private fun setupTestNavigation(view: View) {
        view.findViewById<View>(R.id.btnTestNext).setOnClickListener {
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                updateQuestion()
            } else {
                submitTest()
            }
        }
        view.findViewById<View>(R.id.btnTestBack).setOnClickListener {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                updateQuestion()
            }
        }
    }

    private fun submitTest(isForcedExit: Boolean = false) {
        if (!isAdded) return
        val userProvider = activity as? UserProvider
        val user = userProvider?.getUser() ?: return
        val userId = user.id ?: return
        val lesson = arguments?.getParcelable<Lesson>("lesson") ?: return
        val testId = lesson.test ?: return

        isTestActive = false

        var correctCount = 0
        val results = mutableListOf<ResultItem>()
        val answersRequests = mutableListOf<TestAnswerRequest>()
        
        questions.forEach { question ->
            val selectedAnswer = selectedAnswers[question.id]
            val correctAnswer = question.answers.find { it.is_correct }
            val isCorrect = selectedAnswer?.id == correctAnswer?.id
            if (isCorrect) {
                correctCount++
            }
            results.add(ResultItem(
                questionText = question.text,
                answers = question.answers,
                selectedAnswerText = selectedAnswer?.text ?: "Нет ответа",
                isCorrect = isCorrect
            ))
            answersRequests.add(TestAnswerRequest(
                question_id = question.id,
                chosen_answer_id = selectedAnswer?.id,
                is_correct = isCorrect
            ))
        }

        val finalPercentage = if (questions.isNotEmpty()) (correctCount * 100) / questions.size else 0

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val endTime = sdf.format(Date())

        val testResult = TestResult(
            user_id = userId,
            test_id = testId,
            score = finalPercentage,
            started_at = testStartTime,
            completed_at = endTime,
            answers = answersRequests
        )

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.submitTestResult(testResult)
                if (response.isSuccessful) {
                    val resultResponse = response.body()
                    if (isAdded) Toast.makeText(context, "Результаты сохранены!", Toast.LENGTH_SHORT).show()
                    navigateToTestResultFragment(correctCount, results, lesson.title, resultResponse?.id)
                } else {
                    navigateToTestResultFragment(correctCount, results, lesson.title, null)
                }
            } catch (e: Exception) {
                navigateToTestResultFragment(correctCount, results, lesson.title, null)
            }
        }
    }

    private fun navigateToTestResultFragment(score: Int, results: List<ResultItem>, lessonTitle: String, resultId: String?) {
        if (!isAdded) return
        val bundle = Bundle().apply {
            putInt("score", score)
            putInt("totalQuestions", questions.size)
            putParcelableArrayList("results", ArrayList(results))
            putString("testTitle", "Тест: $lessonTitle")
            putString("resultId", resultId)
        }

        val testResultFragment = TestResultFragment().apply {
            arguments = bundle
        }

        (activity as? SecondActivityWithBottomNavMenu)?.replaceFragment(testResultFragment, bundle)
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == 0) {
            webView?.onResume()
            webView?.resumeTimers()
        }
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
        webView?.pauseTimers()
    }

    override fun onDestroyView() {
        webView?.destroy()
        webView = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(lesson: Lesson, blockTitle: String): LessonDetailFragment {
            return LessonDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("lesson", lesson)
                    putString("block_title", blockTitle)
                }
            }
        }
    }
}
