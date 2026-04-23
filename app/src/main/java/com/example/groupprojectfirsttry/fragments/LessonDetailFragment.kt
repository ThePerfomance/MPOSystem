package com.example.groupprojectfirsttry.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.Lesson
import com.example.groupprojectfirsttry.simpleClasses.Test
import com.google.gson.Gson
import kotlinx.coroutines.launch

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isExoFullscreen) {
                    exitExoFullscreen()
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
        
        val lesson = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("lesson", Lesson::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("lesson")
        }
        
        val blockTitle = arguments?.getString("block_title") ?: "Блок"

        view.findViewById<TextView>(R.id.tvLessonTitleDetail).text = lesson?.title ?: "Урок"
        view.findViewById<TextView>(R.id.tvBlockTitleDetail).text = blockTitle

        val tvSummaryContent = view.findViewById<TextView>(R.id.tvSummaryContent)
        tvSummaryContent.text = lesson?.summary ?: "Нет описания"

        val tvDuration = view.findViewById<TextView>(R.id.tvVideoDurationDetail)
        val minutes = (lesson?.duration ?: 0) / 60
        tvDuration.text = "Продолжительность: $minutes мин"

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            if (isExoFullscreen) exitExoFullscreen()
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
    }

    private fun navigateToTest(lesson: Lesson) {
        val userProvider = activity as? UserProvider
        val user = userProvider?.getUser() ?: return
        
        // Создаем объект теста для перехода
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
                private fun handleUrl(url: String): Boolean {
                    val allowedDomains = listOf("rutube.ru", "vkvideo.ru", "vk.com", "rtbcdn.ru", "vk.me", "yastatic.net", "youtube.com", "youtu.be", "googlevideo.com")
                    return !allowedDomains.any { url.contains(it) } && !url.startsWith("data:")
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = handleUrl(request?.url?.toString() ?: "")
            }

            webChromeClient = object : WebChromeClient() {
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
            val html = "<html><body style='margin:0;padding:0;background:black;'><iframe src='$link' width='100%' height='100%' frameborder='0' allowfullscreen></iframe></body></html>"
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
        val contents = listOf(view.findViewById<View>(R.id.cvVideoContent), view.findViewById<View>(R.id.nsvSummaryContent), view.findViewById<View>(R.id.clTestContent))

        tabs.forEachIndexed { index, layout ->
            layout.setOnClickListener { 
                // Анимация нажатия
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
            
            // Находим иконку и текст внутри вкладки для обновления цвета
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
