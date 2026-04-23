package com.example.groupprojectfirsttry.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LessonDetailFragment : Fragment(R.layout.fragment_lesson_detail) {

    private var webView: WebView? = null
    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var fullscreenContainer: android.widget.FrameLayout? = null
    private var currentTab = 0

    // Test data
    private var questions: List<Question> = emptyList()
    private val selectedAnswers = mutableMapOf<Int, Answer>() // questionId -> chosen answer
    private var testStartTime: String = ""
    
    // UI components for new test design
    private lateinit var llQuestionsContainer: LinearLayout
    private lateinit var btnSubmitTest: MaterialButton
    private lateinit var btnRetryTest: MaterialButton
    private lateinit var cvResultBanner: CardView
    private lateinit var tvResultStatus: TextView
    private lateinit var tvResultScore: TextView
    private lateinit var nsvTestQuestions: View
    private lateinit var llStartTestContainer: View
    
    var isTestActive = false
        private set

    private var isExoFullscreen = false
    
    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isExoFullscreen) {
                    exitExoFullscreen()
                } else if (isTestActive) {
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

        // Player & Video Views
        fullscreenContainer = view.findViewById(R.id.fullscreenContainer)
        playerView = view.findViewById(R.id.exoPlayerView)
        webView = view.findViewById(R.id.webViewRutube)
        
        // Test Views
        llQuestionsContainer = view.findViewById(R.id.llQuestionsContainerDetail)
        btnSubmitTest = view.findViewById(R.id.btnSubmitTestDetail)
        btnRetryTest = view.findViewById(R.id.btnRetryTestDetail)
        cvResultBanner = view.findViewById(R.id.cvResultBannerDetail)
        tvResultStatus = view.findViewById(R.id.tvResultStatusDetail)
        tvResultScore = view.findViewById(R.id.tvResultScoreDetail)
        nsvTestQuestions = view.findViewById(R.id.nsvTestQuestions)
        llStartTestContainer = view.findViewById(R.id.llStartTestContainer)
        
        val lesson = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("lesson", Lesson::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("lesson")
        }
        
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
            if (isExoFullscreen) {
                exitExoFullscreen()
            } else if (isTestActive) {
                showExitConfirmationDialog()
            } else {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        setupVideo(lesson?.video?.finalLink)
        setupTabs(view, lesson)

        // Test logic
        view.findViewById<Button>(R.id.btnStartTest).setOnClickListener {
            startTestSession()
        }

        btnSubmitTest.setOnClickListener {
            if (selectedAnswers.size == questions.size) {
                finishTest()
            } else {
                Toast.makeText(context, "Ответьте на все вопросы", Toast.LENGTH_SHORT).show()
            }
        }

        btnRetryTest.setOnClickListener {
            restartTest()
        }

        if (lesson?.test != null) {
            loadQuestions(lesson.test)
        } else {
            llStartTestContainer.visibility = View.GONE
            view.findViewById<View>(R.id.tvNoTest).visibility = View.VISIBLE
        }
    }

    private fun startTestSession() {
        isTestActive = true
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        testStartTime = sdf.format(Date())
        
        llStartTestContainer.visibility = View.GONE
        nsvTestQuestions.visibility = View.VISIBLE

        if (questions.isNotEmpty()) {
            renderQuestions()
        } else {
            Toast.makeText(context, "Загрузка вопросов...", Toast.LENGTH_SHORT).show()
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
                    setPadding(16, 12, 16, 12)
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

    private fun finishTest() {
        isTestActive = false
        val score = calculateScore()
        val total = questions.size
        
        cvResultBanner.visibility = View.VISIBLE
        tvResultScore.text = "Правильных ответов: $score из $total"
        
        if (score == total) {
            tvResultStatus.text = "✅ Отличный результат!"
            cvResultBanner.setCardBackgroundColor(Color.parseColor("#F1FFF1"))
        } else {
            tvResultStatus.text = "📚 Попробуйте ещё раз"
            cvResultBanner.setCardBackgroundColor(Color.parseColor("#FFF1F1"))
        }

        btnSubmitTest.visibility = View.GONE
        btnRetryTest.visibility = View.VISIBLE

        // Блокируем выбор ответов
        for (i in 0 until llQuestionsContainer.childCount) {
            val rg = llQuestionsContainer.getChildAt(i).findViewById<RadioGroup>(R.id.rgAnswers)
            for (j in 0 until rg.childCount) {
                rg.getChildAt(j).isEnabled = false
            }
        }

        submitTestResults(score)
    }

    private fun restartTest() {
        selectedAnswers.clear()
        cvResultBanner.visibility = View.GONE
        btnRetryTest.visibility = View.GONE
        btnSubmitTest.visibility = View.VISIBLE
        isTestActive = true
        renderQuestions()
    }

    private fun calculateScore(): Int {
        var score = 0
        questions.forEach { question ->
            val selected = selectedAnswers[question.id]
            val correct = question.answers.find { it.is_correct }
            if (selected?.id == correct?.id) score++
        }
        return score
    }

    private fun submitTestResults(score: Int) {
        val userProvider = activity as? UserProvider
        val userId = userProvider?.getUser()?.id ?: return
        val lesson = arguments?.getParcelable<Lesson>("lesson") ?: return
        val testId = lesson.test ?: return

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val endTime = sdf.format(Date())
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
            test_id = testId,
            score = finalPercentage,
            started_at = testStartTime,
            completed_at = endTime,
            answers = answersRequests
        )

        lifecycleScope.launch {
            try {
                ApiClient.apiService.submitTestResult(testResult)
            } catch (e: Exception) {
                Log.e("LessonDetail", "Error sending results", e)
            }
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
                updateTabs(0) // Фоллбэк на видео
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setupVideo(videoLink: String?) {
        var link = videoLink?.trim() ?: ""
        Log.d("VideoDebug", "setupVideo with link: '$link'")
        
        if (link.isEmpty()) {
            webView?.visibility = View.GONE
            playerView?.visibility = View.GONE
            return
        }

        if (link.startsWith("/media/")) {
            link = "http://192.168.31.96:8000$link"
        }

        val uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        val isWebViewVideo = link.contains("rutube.ru", ignoreCase = true) || 
                             link.contains("vk.com", ignoreCase = true) || 
                             link.contains("vkvideo.ru", ignoreCase = true) ||
                             link.contains("youtube.com", ignoreCase = true) ||
                             link.contains("youtu.be", ignoreCase = true) ||
                             link.contains("<iframe", ignoreCase = true) ||
                             uuidPattern.matches(link)

        if (isWebViewVideo) {
            showWebView(link)
        } else {
            showNativePlayer(link)
        }
    }

    private fun showWebView(link: String) {
        releasePlayer()
        webView?.visibility = View.VISIBLE
        playerView?.visibility = View.GONE
        
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
                    val allowedDomains = listOf("rutube.ru", "vkvideo.ru", "vk.com", "rtbcdn.ru", "vk.me", "yastatic.net", "youtube.com", "youtu.be", "googlevideo.com")
                    if (allowedDomains.any { url.contains(it) } || url.startsWith("data:")) {
                        return false 
                    }
                    return true
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
                    activity?.findViewById<View>(R.id.bottom_nav)?.visibility = View.VISIBLE
                    activity?.findViewById<View>(R.id.constraintLayoutUpHead)?.visibility = View.VISIBLE
                    activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            val embedUrl = getVideoEmbedUrl(link)
            val isVk = embedUrl.contains("vk.com", ignoreCase = true) || embedUrl.contains("vkvideo.ru", ignoreCase = true)
            val baseUrl = if (isVk) "https://vkvideo.ru" else "https://rutube.ru"

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
                    <iframe src="$embedUrl" allow="autoplay; encrypted-media; fullscreen; picture-in-picture; screen-wake-lock;" allowfullscreen frameborder="0"></iframe>
                </body>
                </html>
            """.trimIndent()
            
            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
    }

    private fun showNativePlayer(link: String) {
        webView?.visibility = View.GONE
        playerView?.visibility = View.VISIBLE
        initializeExoPlayer(link)
    }

    private fun initializeExoPlayer(url: String) {
        releasePlayer()
        exoPlayer = ExoPlayer.Builder(requireContext()).build().also { player ->
            playerView?.player = player
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            
            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("VideoDebug", "ExoPlayer Error: ${error.message} (Code: ${error.errorCode})")
                    val isFallbackNeeded = error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                                          error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED || 
                                          error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                                          error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
                                          error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                    if (isAdded && isFallbackNeeded) {
                        showWebView(url)
                    }
                }
            })

            playerView?.setFullscreenButtonClickListener { isFullscreen ->
                if (isFullscreen) enterExoFullscreen() else exitExoFullscreen()
            }
            
            player.prepare()
            player.playWhenReady = true
        }
    }

    private fun enterExoFullscreen() {
        isExoFullscreen = true
        val pView = playerView ?: return
        (pView.parent as? ViewGroup)?.removeView(pView)
        fullscreenContainer?.addView(pView)
        fullscreenContainer?.visibility = View.VISIBLE
        
        view?.findViewById<View>(R.id.llHeader)?.visibility = View.GONE
        view?.findViewById<View>(R.id.llTabs)?.visibility = View.GONE
        activity?.findViewById<View>(R.id.bottom_nav)?.visibility = View.GONE
        activity?.findViewById<View>(R.id.constraintLayoutUpHead)?.visibility = View.GONE
        
        activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun exitExoFullscreen() {
        isExoFullscreen = false
        val pView = playerView ?: return
        (pView.parent as? ViewGroup)?.removeView(pView)
        val originalContainer = view?.findViewById<ViewGroup>(R.id.flVideoPlayerContainer)
        originalContainer?.addView(pView)
        fullscreenContainer?.visibility = View.GONE
        
        view?.findViewById<View>(R.id.llHeader)?.visibility = View.VISIBLE
        view?.findViewById<View>(R.id.llTabs)?.visibility = View.VISIBLE
        activity?.findViewById<View>(R.id.bottom_nav)?.visibility = View.VISIBLE
        activity?.findViewById<View>(R.id.constraintLayoutUpHead)?.visibility = View.VISIBLE
        
        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun releasePlayer() {
        if (isExoFullscreen) exitExoFullscreen()
        exoPlayer?.let { player ->
            player.release()
        }
        exoPlayer = null
    }

    private fun getVideoEmbedUrl(url: String): String {
        var trimmed = url.trim()
        val uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        if (uuidPattern.matches(trimmed)) {
            val cleanId = trimmed.replace("-", "")
            return "https://rutube.ru/play/embed/$cleanId/"
        }
        if (trimmed.contains("<iframe", ignoreCase = true)) {
            val srcMatch = "src\\s*=\\s*['\"]([^'\"]+)['\"]".toRegex(RegexOption.IGNORE_CASE).find(trimmed)
            if (srcMatch != null) {
                trimmed = srcMatch.groupValues[1].replace("&amp;", "&")
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
                    val hash = if (trimmed.contains("hash=")) trimmed.substringAfter("hash=").substringBefore("&").substringBefore("/") else null
                    val hashParam = if (hash != null) "&hash=$hash" else ""
                    "https://vkvideo.ru/video_ext.php?oid=$oid&id=$id&hd=2$hashParam"
                } else trimmed
            }
            isRutubeLink -> {
                if (trimmed.contains("play/embed")) return trimmed
                val match = "(?:video|embed)/(?:private/)?([a-f0-9]{32})".toRegex(RegexOption.IGNORE_CASE).find(trimmed)
                if (match != null) {
                    val id = match.groupValues[1]
                    val p = if (trimmed.contains("p=")) "?p=" + trimmed.substringAfter("p=").substringBefore("&").substringBefore("/") else ""
                    "https://rutube.ru/play/embed/$id/$p"
                } else {
                    val fallbackMatch = "([a-f0-9]{32})".toRegex(RegexOption.IGNORE_CASE).find(trimmed)
                    if (fallbackMatch != null) "https://rutube.ru/play/embed/${fallbackMatch.groupValues[1]}/" else trimmed
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

        val hasVideo = !lesson?.video?.finalLink.isNullOrEmpty()
        tabVideo.visibility = if (hasVideo) View.VISIBLE else View.GONE
        currentTab = if (hasVideo) 0 else 1

        val tabs = listOf(tabVideo, tabSummary, tabTest)
        val contents = listOf(contentVideo, contentSummary, contentTest)

        tabs.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                if (currentTab == 2 && isTestActive && index != 2) {
                    showExitConfirmationDialog()
                } else {
                    updateTabs(index)
                }
            }
        }
        updateTabs(currentTab)
    }

    private fun updateTabs(selectedIndex: Int) {
        currentTab = selectedIndex
        val v = view ?: return
        
        val tabVideo = v.findViewById<LinearLayout>(R.id.tabVideo)
        val tabSummary = v.findViewById<LinearLayout>(R.id.tabSummary)
        val tabTest = v.findViewById<LinearLayout>(R.id.tabTest)
        val tabs = listOf(tabVideo, tabSummary, tabTest)
        
        val contentVideo = v.findViewById<View>(R.id.cvVideoContent)
        val contentSummary = v.findViewById<View>(R.id.nsvSummaryContent)
        val contentTest = v.findViewById<View>(R.id.clTestContent)
        val contents = listOf(contentVideo, contentSummary, contentTest)

        tabs.forEachIndexed { index, layout ->
            val isSelected = index == selectedIndex
            layout?.setBackgroundResource(if (isSelected) R.drawable.bg_tab_selected else R.drawable.bg_gray_tag)
            val icon = layout?.getChildAt(0) as? ImageView
            val text = layout?.getChildAt(1) as? TextView
            val color = if (isSelected) resources.getColor(R.color.OnboardingPrimaryTextColor, null) else resources.getColor(R.color.OnboardingSecondaryTextColor, null)
            icon?.setColorFilter(color)
            text?.setTextColor(color)
        }
        
        contents.forEachIndexed { index, contentView ->
            contentView?.visibility = if (index == selectedIndex) View.VISIBLE else View.GONE
        }
        
        if (selectedIndex != 0) {
            webView?.onPause()
            webView?.pauseTimers()
            exoPlayer?.pause()
        } else {
            webView?.onResume()
            webView?.resumeTimers()
            exoPlayer?.play()
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
                llStartTestContainer.visibility = View.GONE
                view?.findViewById<View>(R.id.tvNoTest)?.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e("LessonDetail", "Error loading questions", e)
            Toast.makeText(context, "Ошибка загрузки теста", Toast.LENGTH_SHORT).show()
        } finally {
            pb?.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == 0) {
            webView?.onResume()
            webView?.resumeTimers()
            exoPlayer?.play()
        }
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
        webView?.pauseTimers()
        exoPlayer?.pause()
    }

    override fun onDestroyView() {
        webView?.destroy()
        webView = null
        releasePlayer()
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
