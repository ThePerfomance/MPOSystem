package com.example.groupprojectfirsttry.fragments

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.adapters.AnswersAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TestResult
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.Answer
import com.example.groupprojectfirsttry.simpleClasses.Lesson
import com.example.groupprojectfirsttry.simpleClasses.Question
import com.example.groupprojectfirsttry.simpleClasses.ResultItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LessonDetailFragment : Fragment(R.layout.fragment_lesson_detail) {

    private var currentTab = 1 // Default to Summary (index 1)
    private var webView: WebView? = null
    private var fullscreenContainer: FrameLayout? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // Test related
    private var questions = emptyList<Question>()
    private var currentQuestionIndex = 0
    private val selectedAnswers = mutableMapOf<Int, Answer>()
    private lateinit var answersAdapter: AnswersAdapter
    private var testStartTime: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fullscreenContainer = view.findViewById(R.id.fullscreenContainer)
        val lesson = arguments?.getParcelable<Lesson>("lesson")
        val blockTitle = arguments?.getString("block_title") ?: "Блок"

        val lessonTitle = lesson?.title ?: "Урок"
        view.findViewById<TextView>(R.id.tvLessonTitleDetail).text = lessonTitle
        view.findViewById<TextView>(R.id.tvBlockTitleDetail).text = blockTitle

        // Set summary content
        val tvSummaryContent = view.findViewById<TextView>(R.id.tvSummaryContent)
        tvSummaryContent.text = lesson?.summary ?: "Нет описания"

        // Set duration
        val tvDuration = view.findViewById<TextView>(R.id.tvVideoDurationDetail)
        val minutes = (lesson?.videoDuration ?: 0) / 60
        tvDuration.text = "Продолжительность: $minutes мин"

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        setupWebView(view, lesson?.videoLink)
        setupTabs(view, lesson)
        setupTestNavigation(view)

        if (lesson?.test != null) {
            loadQuestions(lesson.test)
        } else {
            view.findViewById<View>(R.id.llTestContainer).visibility = View.GONE
            view.findViewById<View>(R.id.tvNoTest).visibility = View.VISIBLE
        }
    }

    private fun setupWebView(view: View, videoLink: String?) {
        webView = view.findViewById(R.id.webViewRutube)
        if (videoLink.isNullOrEmpty()) return

        val llHeader = view.findViewById<View>(R.id.llHeader)
        val llTabs = view.findViewById<View>(R.id.llTabs)

        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
            
            webViewClient = WebViewClient()
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

            val embedUrl = getRutubeEmbedUrl(videoLink)
            val html = """
                <html>
                <body style="margin:0;padding:0;background:black;">
                    <iframe width="100%" height="100%" src="$embedUrl" style="border: none;" allow="clipboard-write; autoplay" allowFullScreen></iframe>
                </body>
                </html>
            """.trimIndent()
            loadDataWithBaseURL("https://rutube.ru", html, "text/html", "UTF-8", null)
        }
    }

    private fun getRutubeEmbedUrl(url: String): String {
        if (url.contains("play/embed")) return url
        val cleanUrl = url.substringBefore("?")
        val segments = cleanUrl.split("/").filter { it.isNotEmpty() }
        val videoId = segments.lastOrNull()
        return if (videoId != null && videoId.length >= 32) "https://rutube.ru/play/embed/$videoId/" else url
    }

    private fun setupTabs(view: View, lesson: Lesson?) {
        val tabVideo = view.findViewById<LinearLayout>(R.id.tabVideo)
        val tabSummary = view.findViewById<LinearLayout>(R.id.tabSummary)
        val tabTest = view.findViewById<LinearLayout>(R.id.tabTest)

        val contentVideo = view.findViewById<View>(R.id.cvVideoContent)
        val contentSummary = view.findViewById<View>(R.id.nsvSummaryContent)
        val contentTest = view.findViewById<View>(R.id.clTestContent)

        val hasVideo = !lesson?.videoLink.isNullOrEmpty()
        tabVideo.visibility = if (hasVideo) View.VISIBLE else View.GONE
        currentTab = if (hasVideo) 0 else 1

        val tabs = listOf(tabVideo, tabSummary, tabTest)
        val contents = listOf(contentVideo, contentSummary, contentTest)

        tabs.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                updateTabs(index, tabs, contents)
            }
        }
        updateTabs(currentTab, tabs, contents)
    }

    private fun updateTabs(selectedIndex: Int, tabs: List<LinearLayout>, contents: List<View>) {
        currentTab = selectedIndex
        tabs.forEachIndexed { index, layout ->
            val isSelected = index == selectedIndex
            layout.setBackgroundResource(if (isSelected) R.drawable.bg_tab_selected else R.drawable.bg_gray_tag)
            val icon = layout.getChildAt(0) as? android.widget.ImageView
            val text = layout.getChildAt(1) as? TextView
            val color = if (isSelected) resources.getColor(R.color.OnboardingPrimaryTextColor, null) else resources.getColor(R.color.OnboardingSecondaryTextColor, null)
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
        
        if (selectedIndex == 2 && testStartTime.isEmpty()) {
            testStartTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        }
    }

    private fun loadQuestions(testId: Int) = viewLifecycleOwner.lifecycleScope.launch {
        val pb = view?.findViewById<ProgressBar>(R.id.pbTestLoading)
        pb?.visibility = View.VISIBLE
        try {
            val response = ApiClient.apiService.getQuestions(testId)
            if (response.isNotEmpty()) {
                questions = response
                updateQuestion()
            } else {
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

        answersAdapter = AnswersAdapter(question.answers) { answer ->
            selectedAnswers[question.id] = answer
            view.findViewById<Button>(R.id.btnTestNext).isEnabled = true
        }
        
        // Restore selection if exists
        val previousAnswer = selectedAnswers[question.id]
        // Note: AnswersAdapter might need adjustment to support pre-selection if we want back navigation to show it.
        // For now, let's just use it as is.

        view.findViewById<RecyclerView>(R.id.rvAnswers).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = answersAdapter
        }

        view.findViewById<Button>(R.id.btnTestBack).isEnabled = currentQuestionIndex > 0
        view.findViewById<Button>(R.id.btnTestNext).isEnabled = selectedAnswers.containsKey(question.id)
    }

    private fun setupTestNavigation(view: View) {
        view.findViewById<Button>(R.id.btnTestBack).setOnClickListener {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                updateQuestion()
            }
        }

        view.findViewById<Button>(R.id.btnTestNext).setOnClickListener {
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                updateQuestion()
            } else {
                finishTest()
            }
        }
    }

    private fun finishTest() {
        val score = calculateScore()
        val results = mutableListOf<ResultItem>()
        for (question in questions) {
            val selectedAnswer = selectedAnswers[question.id]
            val correctAnswer = question.answers.find { it.is_correct }
            results.add(ResultItem(
                questionText = question.text,
                answers = question.answers,
                selectedAnswerText = selectedAnswer?.text ?: "Не выбран",
                isCorrect = selectedAnswer?.id == correctAnswer?.id
            ))
        }

        sendResultsToServer(score)
        navigateToTestResultFragment(score, results)
    }

    private fun calculateScore(): Int {
        var score = 0
        for ((questionId, selectedAnswer) in selectedAnswers) {
            val correctAnswer = questions.find { it.id == questionId }?.answers?.find { it.is_correct }
            if (selectedAnswer.id == correctAnswer?.id) score++
        }
        return score
    }

    private fun sendResultsToServer(score: Int) {
        val user = (requireActivity() as? UserProvider)?.getUser() ?: return
        val lesson = arguments?.getParcelable<Lesson>("lesson") ?: return
        val testId = lesson.test ?: return

        val completedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        val result = TestResult(user.id!!, testId, score, testStartTime, completedAt)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ApiClient.apiService.submitTestResult(result)
            } catch (e: Exception) {
                Log.e("LessonDetail", "Error submitting result", e)
            }
        }
    }

    private fun navigateToTestResultFragment(score: Int, results: List<ResultItem>) {
        val lesson = arguments?.getParcelable<Lesson>("lesson")
        val bundle = Bundle().apply {
            putInt("score", score)
            putInt("totalQuestions", questions.size)
            putParcelableArrayList("results", ArrayList(results))
            putString("testTitle", lesson?.title ?: "Результат теста")
        }
        val fragment = TestResultFragment().apply { arguments = bundle }
        (requireActivity() as? SecondActivityWithBottomNavMenu)?.replaceFragment(fragment, bundle)
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
        webView?.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == 0) {
            webView?.onResume()
            webView?.resumeTimers()
        }
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