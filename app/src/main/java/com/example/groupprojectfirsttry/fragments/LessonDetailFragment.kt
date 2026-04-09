package com.example.groupprojectfirsttry.fragments

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.Lesson

class LessonDetailFragment : Fragment(R.layout.fragment_lesson_detail) {

    private var currentTab = 1 // Default to Summary (index 1)
    private var webView: WebView? = null
    private var fullscreenContainer: FrameLayout? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fullscreenContainer = view.findViewById(R.id.fullscreenContainer)
        val lesson = arguments?.getParcelable<Lesson>("lesson")
        val blockTitle = arguments?.getString("block_title") ?: "Блок"

        val lessonTitle = lesson?.title ?: "Урок"
        view.findViewById<TextView>(R.id.tvLessonTitleDetail).text = lessonTitle
        view.findViewById<TextView>(R.id.tvBlockTitleDetail).text = blockTitle

        // Set summary content from DB
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
            // Устанавливаем Desktop User-Agent, чтобы Rutube не редиректил на мобильную версию сайта
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
                    
                    // Скрываем все элементы интерфейса
                    llHeader.visibility = View.GONE
                    llTabs.visibility = View.GONE
                    activity?.findViewById<View>(R.id.bottom_nav)?.visibility = View.GONE
                    activity?.findViewById<View>(R.id.constraintLayoutUpHead)?.visibility = View.GONE
                    
                    // Скрываем системную статус-панель
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
                    
                    // Возвращаем элементы интерфейса
                    llHeader.visibility = View.VISIBLE
                    llTabs.visibility = View.VISIBLE
                    activity?.findViewById<View>(R.id.bottom_nav)?.visibility = View.VISIBLE
                    activity?.findViewById<View>(R.id.constraintLayoutUpHead)?.visibility = View.VISIBLE
                    
                    // Показываем системную статус-панель обратно
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
        
        // Извлекаем ID видео, игнорируя параметры запроса и лишние слеши
        val cleanUrl = url.substringBefore("?")
        val segments = cleanUrl.split("/").filter { it.isNotEmpty() }
        val videoId = segments.lastOrNull()
        
        return if (videoId != null && videoId.length >= 32) {
            "https://rutube.ru/play/embed/$videoId/"
        } else {
            url
        }
    }

    private fun setupTabs(view: View, lesson: Lesson?) {
        val tabVideo = view.findViewById<LinearLayout>(R.id.tabVideo)
        val tabSummary = view.findViewById<LinearLayout>(R.id.tabSummary)
        val tabTest = view.findViewById<LinearLayout>(R.id.tabTest)

        val contentVideo = view.findViewById<View>(R.id.cvVideoContent)
        val contentSummary = view.findViewById<View>(R.id.tvSummaryContent)
        val contentTest = view.findViewById<View>(R.id.tvTestContent)

        // Hide video tab if no link
        val hasVideo = !lesson?.videoLink.isNullOrEmpty()
        tabVideo.visibility = if (hasVideo) View.VISIBLE else View.GONE
        
        // If no video, default tab is Summary
        if (!hasVideo) {
            currentTab = 1
        } else {
            currentTab = 0
        }

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
            
            // Update children colors (Icon and Text)
            val icon = layout.getChildAt(0) as? android.widget.ImageView
            val text = layout.getChildAt(1) as? TextView
            
            val color = if (isSelected) 
                resources.getColor(R.color.OnboardingPrimaryTextColor, null)
            else 
                resources.getColor(R.color.OnboardingSecondaryTextColor, null)
            
            icon?.setColorFilter(color)
            text?.setTextColor(color)
            text?.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }

        contents.forEachIndexed { index, contentView ->
            contentView.visibility = if (index == selectedIndex) View.VISIBLE else View.GONE
        }
        
        // Pause/Resume video based on tab
        if (selectedIndex != 0) {
            webView?.onPause()
            webView?.pauseTimers()
        } else {
            webView?.onResume()
            webView?.resumeTimers()
        }
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
