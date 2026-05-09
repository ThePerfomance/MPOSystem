package com.example.groupprojectfirsttry.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AlignmentSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.BuildConfig
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.adapters.TheoriaAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.Lesson
import com.example.groupprojectfirsttry.simpleClasses.Test
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream

class LessonDetailFragment : Fragment(R.layout.fragment_lesson_detail) {

    private var webView: WebView? = null
    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var fullscreenContainer: android.widget.FrameLayout? = null
    private var currentTab = 0

    private var isExoFullscreen = false
    private var backPressedCallback: OnBackPressedCallback? = null

    // For Docx/Theoria content
    private lateinit var theoriaAdapter: TheoriaAdapter
    private lateinit var rvTheoria: RecyclerView
    private lateinit var nsvSummary: View
    private lateinit var tvSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isExoFullscreen) {
                    exitExoFullscreen()
                } else if (customView != null) {
                    webView?.webChromeClient?.onHideCustomView()
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
        playerView = view.findViewById(R.id.exoPlayerView)
        webView = view.findViewById(R.id.webViewRutube)
        
        // Views for both content types
        rvTheoria = view.findViewById(R.id.rvTheoriaContent)
        nsvSummary = view.findViewById(R.id.nsvSummaryContent)
        tvSummary = view.findViewById(R.id.tvSummaryContent)
        
        theoriaAdapter = TheoriaAdapter()
        rvTheoria.adapter = theoriaAdapter
        rvTheoria.layoutManager = LinearLayoutManager(requireContext())

        val lesson = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("lesson", Lesson::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("lesson")
        }
        
        val blockTitle = arguments?.getString("block_title") ?: "Блок"

        view.findViewById<TextView>(R.id.tvLessonTitleDetail).text = lesson?.title ?: "Урок"
        view.findViewById<TextView>(R.id.tvBlockTitleDetail).text = blockTitle

        val tvDuration = view.findViewById<TextView>(R.id.tvVideoDurationDetail)
        val minutes = (lesson?.duration ?: 0) / 60
        tvDuration.text = "Продолжительность: $minutes мин"

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            if (isExoFullscreen) exitExoFullscreen()
            else if (customView != null) webView?.webChromeClient?.onHideCustomView()
            else requireActivity().supportFragmentManager.popBackStack()
        }

        setupVideo(lesson?.video?.finalLink)
        setupTabs(view, lesson)

        // Кнопка перехода к тесту
        view.findViewById<Button>(R.id.btnStartTest).setOnClickListener {
            if (lesson?.test != null) {
                navigateToTest(lesson)
            }
        }

        if (lesson?.test == null) {
            view.findViewById<View>(R.id.llStartTestContainer).visibility = View.GONE
            view.findViewById<View>(R.id.tvNoTest).visibility = View.VISIBLE
        }

        // Logic choice based on flavor (standard/impuls)
        if (lesson != null) {
            if (BuildConfig.USE_DOCX_THEORY) {
                view.findViewById<TextView>(R.id.tvTabSummary).text = "Тема"
                loadDocxForLesson(lesson.title)
            } else {
                view.findViewById<TextView>(R.id.tvTabSummary).text = "Саммари"
                tvSummary.text = lesson.summary ?: "Нет описания"
            }
        }
    }

    private fun loadDocxForLesson(lessonTitle: String) {
        val fileName = when {
            lessonTitle.contains("Введение", ignoreCase = true) -> "0Vvedenie.docx"
            lessonTitle.contains("Основы языка разметки HTML", ignoreCase = true) -> "1VvedenieHTML.docx"
            lessonTitle.contains("Работа с формами", ignoreCase = true) -> "2RabotaSFormami.docx"
            lessonTitle.contains("Семантическая верстка", ignoreCase = true) -> "3VerstkaStranits.docx"
            lessonTitle.contains("Каскадные таблицы стилей", ignoreCase = true) -> "4CSSCascadeTables.docx"
            lessonTitle.contains("Фильтры в CSS", ignoreCase = true) -> "5CSSFilters.docx"
            lessonTitle.contains("Блоковые элементы", ignoreCase = true) -> "6CSSBlockElements.docx"
            lessonTitle.contains("Трансформации", ignoreCase = true) -> "7TransformationAndAnimation.docx"
            lessonTitle.contains("Адаптивная верстка", ignoreCase = true) -> "8AdaptiveVerstka.docx"
            lessonTitle.contains("Flexbox", ignoreCase = true) -> "9FlexibleMaket.docx"
            lessonTitle.contains("Grid Layout", ignoreCase = true) -> "10GridLayout.docx"
            lessonTitle.contains("Переменные в CSS", ignoreCase = true) -> "11UsingPeremenInCSS.docx"
            lessonTitle.contains("Заключение", ignoreCase = true) -> "99FinalWords.docx"
            else -> null
        }

        if (fileName != null) {
            loadFile(fileName)
        }
    }

    private fun loadFile(fileName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = requireContext().assets.open(fileName)
                val document = XWPFDocument(inputStream)
                val newItems = mutableListOf<Any>()
                
                for (paragraph in document.paragraphs) {
                    val spannable = SpannableStringBuilder()
                    val rawParagraphStyle = paragraph.style?.lowercase() ?: ""
                    val isHeading = rawParagraphStyle.contains("heading") ||
                            rawParagraphStyle.contains("заголовок") ||
                            rawParagraphStyle.contains("глава") ||
                            rawParagraphStyle == "title"

                    paragraph.runs.forEach { run ->
                        val text = run.text() ?: ""
                        val start = spannable.length
                        spannable.append(text)
                        if (run.isBold) {
                            spannable.setSpan(StyleSpan(Typeface.BOLD), start, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        if (run.isItalic) {
                            spannable.setSpan(StyleSpan(Typeface.ITALIC), start, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }

                    if (spannable.isNotEmpty()) {
                        if (isHeading) {
                            spannable.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        } else {
                            spannable.setSpan(LeadingMarginSpan.Standard((40 * resources.displayMetrics.density).toInt(), 0), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        newItems.add(spannable)
                    }

                    paragraph.runs.flatMap { it.embeddedPictures }.forEach { picture ->
                        BitmapFactory.decodeStream(ByteArrayInputStream(picture.pictureData.data))?.let {
                            newItems.add(it)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    theoriaAdapter.setItems(newItems)
                }
                document.close()
                inputStream.close()
            } catch (e: Exception) {
                Log.e("LessonDetail", "Error loading docx: ${e.message}")
            }
        }
    }

    private fun navigateToTest(lesson: Lesson) {
        val userProvider = activity as? UserProvider
        val user = userProvider?.getUser() ?: return
        
        val testObject = Test(
            id = lesson.test!!,
            title = lesson.title,
            description = lesson.summary ?: "",
            subjectName = "",
            progress = 0
        )
        
        val bundle = Bundle().apply {
            putParcelable("test", testObject)
            putParcelable("user", user)
        }
        
        val testPassFragment = TestPassFragment().apply {
            arguments = bundle
        }
        
        (activity as? SecondActivityWithBottomNavMenu)?.replaceFragment(testPassFragment, bundle)
    }

    private fun setupVideo(videoLink: String?) {
        var link = videoLink?.trim() ?: ""
        if (link.isEmpty()) {
            webView?.visibility = View.GONE
            playerView?.visibility = View.GONE
            return
        }

        if (link.startsWith("/media/")) {
            link = "http://10.0.2.2:8000$link"
        }

        val uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        
        if (uuidPattern.matches(link)) {
            link = "https://rutube.ru/play/embed/$link"
        } else if (link.contains("rutube.ru/video/")) {
            link = link.replace("rutube.ru/video/", "rutube.ru/play/embed/")
        } else if (link.contains("youtube.com/watch?v=")) {
            val videoId = link.substringAfter("v=").substringBefore("&")
            link = "https://www.youtube.com/embed/$videoId"
        } else if (link.contains("youtu.be/")) {
            val videoId = link.substringAfter("youtu.be/").substringBefore("?")
            link = "https://www.youtube.com/embed/$videoId"
        }

        val isWebViewVideo = link.contains("rutube.ru", ignoreCase = true) || 
                             link.contains("vk.com", ignoreCase = true) || 
                             link.contains("vkvideo.ru", ignoreCase = true) ||
                             link.contains("youtube.com", ignoreCase = true) ||
                             link.contains("youtu.be", ignoreCase = true) ||
                             link.contains("<iframe", ignoreCase = true)

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
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = true
            }
            
            webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    Log.e("LessonDetail", "WebView render process gone. Crash: ${detail?.didCrash()}")
                    view?.loadUrl(link)
                    return true
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: ""
                    
                    val isEmbed = url.contains("/play/embed/") || url.contains("/embed/") || url.contains("video_ext.php")
                    if (isEmbed) return false
                    
                    if (url.contains("rutube.ru/video/") || url.contains("rutube.ru/user/") || 
                        url.contains("youtube.com/watch") || url.contains("vk.com/video") ||
                        url.contains("rutube.ru/channel/")) {
                        return true
                    }

                    val allowedDomains = listOf("rutube.ru", "vkvideo.ru", "vk.com", "rtbcdn.ru", "vk.me", "yastatic.net", "youtube.com", "youtu.be", "googlevideo.com")
                    val isAllowedDomain = allowedDomains.any { url.contains(it) }

                    if (!isAllowedDomain && !url.startsWith("data:")) {
                        return true
                    }

                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectHideElementsScript(view)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                    return false
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    customView = view
                    fullscreenContainer?.addView(customView)
                    fullscreenContainer?.visibility = View.VISIBLE
                    customViewCallback = callback
                    activity?.findViewById<View>(R.id.bottom_nav)?.visibility = View.GONE
                    activity?.findViewById<View>(R.id.constraintLayoutUpHead)?.visibility = View.GONE
                    activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }

                override fun onHideCustomView() {
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

            val isVk = link.contains("vk")
            val baseUrl = if (isVk) "https://vkvideo.ru" else "https://rutube.ru"
            val html = if (link.contains("<iframe", ignoreCase = true)) {
                "<html><body style='margin:0;padding:0;background:black;'>$link</body></html>"
            } else {
                "<html><body style='margin:0;padding:0;background:black;'><iframe src='$link' width='100%' height='100%' frameborder='0' allowfullscreen></iframe></body></html>"
            }
            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
    }

    private fun injectHideElementsScript(view: WebView?) {
        val js = """
            (function() {
                const selectors = [
                    '.rt-v-player-overlay__logo', 
                    '.rt-v-player-overlay__share', 
                    '.rt-v-player-overlay__title',
                    '.rt-v-player-overlay__more',
                    '.rt-v-player-overlay__recommendations',
                    '.player-video-ad-container',
                    '.rt-v-player-ad-overlay',
                    '.b-video-ad-overlay',
                    '[class*="ad-overlay"]',
                    '[class*="player-ad"]',
                    '.vkuiIcon--cancel_24'
                ];
                function hide() {
                    selectors.forEach(s => {
                        document.querySelectorAll(s).forEach(el => {
                            if (el.style.display !== 'none') {
                                el.style.setProperty('display', 'none', 'important');
                                el.style.setProperty('pointer-events', 'none', 'important');
                                el.style.setProperty('visibility', 'hidden', 'important');
                                el.style.setProperty('opacity', '0', 'important');
                                el.style.setProperty('z-index', '-1', 'important');
                            }
                        });
                    });
                }
                hide();
                const observer = new MutationObserver(hide);
                observer.observe(document, { childList: true, subtree: true });
                setInterval(hide, 2000);
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
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
            player.setMediaItem(MediaItem.fromUri(url))
            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (isAdded) showWebView(url)
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
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun exitExoFullscreen() {
        isExoFullscreen = false
        val pView = playerView ?: return
        (pView.parent as? ViewGroup)?.removeView(pView)
        view?.findViewById<ViewGroup>(R.id.flVideoPlayerContainer)?.addView(pView)
        fullscreenContainer?.visibility = View.GONE
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun releasePlayer() {
        if (isExoFullscreen) exitExoFullscreen()
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun setupTabs(view: View, lesson: Lesson?) {
        val tabs = listOf(view.findViewById<View>(R.id.tabVideo), view.findViewById<View>(R.id.tabSummary), view.findViewById<View>(R.id.tabTest))
        
        // Dynamic content choosing
        val contentForTheory = if (BuildConfig.USE_DOCX_THEORY) rvTheoria else nsvSummary
        val contents = listOf(view.findViewById<View>(R.id.cvVideoContent), contentForTheory, view.findViewById<View>(R.id.clTestContent))

        tabs.forEachIndexed { index, layout ->
            layout.setOnClickListener { 
                layout.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        layout.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        updateTabs(index, tabs, contents)
                    }
                    .start()
            }
        }
        updateTabs(0, tabs, contents)
    }

    private fun updateTabs(selectedIndex: Int, tabs: List<View>, contents: List<View>) {
        currentTab = selectedIndex
        tabs.forEachIndexed { index, layout ->
            val isSelected = index == selectedIndex
            layout.setBackgroundResource(if (isSelected) R.drawable.bg_tab_selected else R.drawable.bg_gray_tag)
            
            val container = layout as? ViewGroup
            if (container != null) {
                val icon = container.getChildAt(0) as? ImageView
                val text = container.getChildAt(1) as? TextView
                
                val color = if (isSelected) {
                    resources.getColor(R.color.OnboardingPrimaryTextColor, null)
                } else {
                    resources.getColor(R.color.OnboardingSecondaryTextColor, null)
                }
                
                icon?.setColorFilter(color)
                text?.setTextColor(color)
            }
        }
        contents.forEachIndexed { index, contentView ->
            contentView.visibility = if (index == selectedIndex) View.VISIBLE else View.GONE
        }
        if (selectedIndex != 0) {
            webView?.onPause()
            exoPlayer?.pause()
        } else {
            webView?.onResume()
            exoPlayer?.play()
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == 0) {
            webView?.onResume()
            exoPlayer?.play()
        }
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroyView() {
        webView?.destroy()
        releasePlayer()
        super.onDestroyView()
    }

    companion object {
        fun newInstance(lesson: Lesson, blockTitle: String): LessonDetailFragment = LessonDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable("lesson", lesson)
                putString("block_title", blockTitle)
            }
        }
    }
}
