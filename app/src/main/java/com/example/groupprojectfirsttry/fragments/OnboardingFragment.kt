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
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.simpleClasses.Block
import com.example.groupprojectfirsttry.simpleClasses.Lesson
import kotlinx.coroutines.launch
import java.util.UUID

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private lateinit var llBlocksContainer: LinearLayout
    private val apiService = ApiClient.apiService
    private var subjectId: UUID? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        llBlocksContainer = view.findViewById(R.id.llBlocksContainer)

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                Log.d("OnboardingFragment", "Fetching subjects...")
                val subjects = apiService.getSubjects()
                Log.d("OnboardingFragment", "Successfully fetched ${subjects.size} subjects")

                if (subjects.isNotEmpty()) {
                    // Используем первый предмет и сохраняем его ID в глобальную переменную
                    val subject = subjects[2]
                    subjectId = subject.id
                    
                    Log.d("OnboardingFragment", "Fetching blocks for subject: ${subject.name} (ID: $subjectId)")
                    
                    val blocks = apiService.getBlocksBySubject(subjectId!!)
                    Log.d("OnboardingFragment", "Successfully fetched ${blocks.size} blocks")
                    renderBlocks(blocks)
                } else {
                    Log.w("OnboardingFragment", "Subjects list is empty")
                    Toast.makeText(requireContext(), "Предметы не найдены", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("OnboardingFragment", "Error loading data", e)
                if (e is retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("OnboardingFragment", "HTTP Error ${e.code()}: $errorBody")
                }
                Toast.makeText(requireContext(), "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderBlocks(blocks: List<Block>) {
        llBlocksContainer.removeAllViews()
        
        blocks.sortedBy { it.position }.forEach { block ->
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
            
            // Загружаем уроки для каждого блока
            lifecycleScope.launch {
                try {
                    Log.d("OnboardingFragment", "Fetching lessons for block: ${block.title} (ID: ${block.id})")
                    val lessons = apiService.getLessonsByBlock(block.id)
                    tvProgress.text = getString(R.string.onboarding_lessons_count, 0, lessons.size)
                    renderLessons(llLessonsContainer, lessons, block.title)
                    
                    if (block.finalTestId != null) {
                        val finalTestView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_onboarding_final_test, llLessonsContainer, false)
                        llLessonsContainer.addView(finalTestView)
                    }
                } catch (e: Exception) {
                    Log.e("OnboardingFragment", "Error loading lessons for block ${block.id}", e)
                }
            }

            // Expand/Collapse logic
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

    private fun renderLessons(container: LinearLayout, lessons: List<Lesson>, blockTitle: String) {
        container.removeAllViews()
        lessons.sortedBy { it.position }.forEach { lesson ->
            val lessonView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_onboarding_lesson, container, false)
            
            val title = lesson.title
            lessonView.findViewById<TextView>(R.id.tvLessonTitle).text = title
            
            val minutes = lesson.duration / 60
            lessonView.findViewById<TextView>(R.id.tvLessonDuration).text = "$minutes мин"
            
            val type = if (!lesson.videoLink.isNullOrEmpty()) "Видео" else "Чтение"
            lessonView.findViewById<TextView>(R.id.tvLessonType).text = type
            
            if (!lesson.isPublished) {
                lessonView.alpha = 0.5f
                lessonView.findViewById<ImageView>(R.id.ivLessonStatus).setImageResource(R.drawable.ic_lock_closed)
            } else {
                lessonView.setOnClickListener {
                    val detailFragment = LessonDetailFragment.newInstance(lesson, blockTitle)
                    (activity as? SecondActivityWithBottomNavMenu)?.replaceFragment(detailFragment, detailFragment.arguments)
                }
            }

            container.addView(lessonView)
        }
    }
}
