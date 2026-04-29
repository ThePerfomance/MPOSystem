package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.api.*
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.Block
import com.example.groupprojectfirsttry.simpleClasses.Lesson
import com.example.groupprojectfirsttry.simpleClasses.Test
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.UUID

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private lateinit var llBlocksContainer: LinearLayout
    private val apiService = ApiClient.apiService
    private var subjectId: UUID? = null
    
    private lateinit var shimmerOnboarding: ShimmerFrameLayout
    private lateinit var nsvOnboardingContent: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        llBlocksContainer = view.findViewById(R.id.llBlocksContainer)
        shimmerOnboarding = view.findViewById(R.id.shimmer_onboarding)
        nsvOnboardingContent = view.findViewById(R.id.nsvOnboardingContent)

        loadData()
    }

    private fun startLoading() {
        shimmerOnboarding.visibility = View.VISIBLE
        shimmerOnboarding.startShimmer()
        nsvOnboardingContent.visibility = View.GONE
    }

    private fun stopLoading() {
        shimmerOnboarding.stopShimmer()
        shimmerOnboarding.visibility = View.GONE
        nsvOnboardingContent.visibility = View.VISIBLE
    }

    private fun loadData() {
        val userProvider = activity as? UserProvider
        val user = userProvider?.getUser()

        startLoading()

        lifecycleScope.launch {
            try {
                Log.d("OnboardingFragment", "Fetching subjects...")
                val subjects = apiService.getSubjects()
                
                if (subjects.isNotEmpty()) {
                    val subject = if (subjects.size > 2) subjects[2] else subjects[0]
                    subjectId = subject.id
                    
                    val blocks = apiService.getBlocksBySubject(subjectId!!).sortedBy { it.position }
                    val userResults = user?.id?.let { apiService.getUserTestResults(it) } ?: emptyList()
                    val finishedTestIds = userResults.map { it.test_id }.toSet()

                    val blocksWithLessons = blocks.map { block ->
                        async { 
                            try {
                                block to apiService.getLessonsByBlock(block.id)
                            } catch (e: Exception) {
                                Log.e("OnboardingFragment", "Error loading lessons for block ${block.id}", e)
                                block to emptyList<Lesson>()
                            }
                        }
                    }.awaitAll()

                    if (isAdded) {
                        renderBlocksWithLessons(blocksWithLessons, finishedTestIds)
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Предметы не найдены", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                handleNetworkError(e)
            } finally {
                if (isAdded) stopLoading()
            }
        }
    }

    private fun handleNetworkError(e: Exception) {
        Log.e("OnboardingFragment", "Network error", e)
        if (!isAdded) return
        
        val message = when (e) {
            is NoConnectivityException -> e.message
            is ServerUnavailableException -> e.message
            is ApiException -> "Ошибка сервера: ${e.code}"
            else -> "Произошла ошибка при загрузке обучения"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun renderBlocksWithLessons(data: List<Pair<Block, List<Lesson>>>, finishedTestIds: Set<Int>) {
        if (!isAdded) return
        llBlocksContainer.removeAllViews()
        
        var firstUnfinishedIndex = data.indexOfFirst { (block, lessons) ->
            val finishedInBlock = lessons.count { it.test != null && finishedTestIds.contains(it.test) }
            finishedInBlock < lessons.size
        }
        
        if (firstUnfinishedIndex == -1) firstUnfinishedIndex = -1 

        data.forEachIndexed { index, (block, lessons) ->
            val blockView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_onboarding_block, llBlocksContainer, false)

            val tvNumber = blockView.findViewById<TextView>(R.id.tvBlockNumber)
            val tvTitle = blockView.findViewById<TextView>(R.id.tvBlockTitle)
            val tvProgress = blockView.findViewById<TextView>(R.id.tvBlockProgress)
            val ivChevron = blockView.findViewById<ImageView>(R.id.ivChevron)
            val rlHeader = blockView.findViewById<RelativeLayout>(R.id.rlBlockHeader)
            val llLessonsContainer = blockView.findViewById<LinearLayout>(R.id.llLessonsContainer)

            tvNumber.text = (block.position + 1).toString()
            tvTitle.text = block.title
            
            val finishedInBlock = lessons.count { it.test != null && finishedTestIds.contains(it.test) }
            tvProgress.text = getString(R.string.onboarding_lessons_count, finishedInBlock, lessons.size)
            
            renderLessons(llLessonsContainer, lessons, block.title, finishedTestIds)
            
            if (block.finalTestId != null) {
                val finalTestView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_onboarding_final_test, llLessonsContainer, false)
                
                val isUnlocked = finishedInBlock >= lessons.size
                val isFinished = finishedTestIds.contains(block.finalTestId)

                if (isFinished) {
                    finalTestView.findViewById<ImageView>(R.id.ivFinalTestStatus)?.apply {
                        setImageResource(R.drawable.ic_circle_filled)
                        setColorFilter(resources.getColor(R.color.AccentColor, null))
                    }
                }

                if (!isUnlocked) {
                    finalTestView.alpha = 0.5f
                } else {
                    finalTestView.setOnClickListener {
                        startFinalTest(block)
                    }
                }
                
                llLessonsContainer.addView(finalTestView)
            }

            if (index == firstUnfinishedIndex) {
                llLessonsContainer.isVisible = true
                ivChevron.rotation = 180f
            }

            rlHeader.setOnClickListener {
                val willBeVisible = !llLessonsContainer.isVisible
                TransitionManager.beginDelayedTransition(llBlocksContainer, AutoTransition())
                llLessonsContainer.isVisible = willBeVisible
                ivChevron.animate()
                    .rotation(if (willBeVisible) 180f else 0f)
                    .setDuration(300)
                    .start()
            }

            llBlocksContainer.addView(blockView)
        }
    }

    private fun renderLessons(container: LinearLayout, lessons: List<Lesson>, blockTitle: String, finishedTestIds: Set<Int>) {
        if (!isAdded) return
        container.removeAllViews()
        lessons.sortedBy { it.position }.forEach { lesson ->
            val lessonView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_onboarding_lesson, container, false)
            
            val title = lesson.title
            lessonView.findViewById<TextView>(R.id.tvLessonTitle).text = title
            
            val minutes = lesson.duration / 60
            lessonView.findViewById<TextView>(R.id.tvLessonDuration).text = "$minutes мин"
            
            // Исправлено: video теперь объект Video, проверяем link
            val type = if (!lesson.video?.finalLink.isNullOrEmpty()) "Видео" else "Чтение"
            lessonView.findViewById<TextView>(R.id.tvLessonType).text = type
            
            val ivStatus = lessonView.findViewById<ImageView>(R.id.ivLessonStatus)
            val isFinished = lesson.test != null && finishedTestIds.contains(lesson.test)

            if (isFinished) {
                ivStatus.setImageResource(R.drawable.ic_circle_filled)
                ivStatus.setColorFilter(resources.getColor(R.color.AccentColor, null))
            } else {
                ivStatus.setImageResource(R.drawable.ic_circle_outline)
                ivStatus.setColorFilter(resources.getColor(R.color.OnboardingSecondaryTextColor, null))
            }
            
            if (!lesson.isPublished) {
                lessonView.alpha = 0.5f
                ivStatus.setImageResource(R.drawable.ic_lock_closed)
            } else {
                lessonView.setOnClickListener {
                    val detailFragment = LessonDetailFragment.newInstance(lesson, blockTitle)
                    (activity as? SecondActivityWithBottomNavMenu)?.replaceFragment(detailFragment, detailFragment.arguments)
                }
            }

            container.addView(lessonView)
        }
    }

    private fun startFinalTest(block: Block) {
        val testId = block.finalTestId ?: return
        val user = (activity as? UserProvider)?.getUser() ?: return
        
        // Создаем временный объект Test для перехода в TestPassFragment
        val testObject = Test(
            id = testId,
            title = "Финальный тест: ${block.title}",
            description = block.description,
            subjectName = "", // Можно подтянуть имя предмета, если нужно
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
}
