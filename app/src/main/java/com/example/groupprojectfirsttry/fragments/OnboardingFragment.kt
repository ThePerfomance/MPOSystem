package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.R

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private lateinit var llBlocksContainer: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        llBlocksContainer = view.findViewById(R.id.llBlocksContainer)

        // Mock data - Now all blocks will have a final test by default
        val blocks = listOf(
            OnboardingBlock(
                1, "Введение в компанию", listOf(
                    Lesson("История и ценности компании", "15 мин", "Видео"),
                    Lesson("Структура и команды", "20 мин", "Видео", isLocked = true),
                    Lesson("Корпоративная культура", "18 мин", "Видео", isLocked = true)
                )
            ),
            OnboardingBlock(
                2, "Технический стек", listOf(
                    Lesson("Основные инструменты", "10 мин", "Чтение"),
                    Lesson("Процесс разработки", "25 мин", "Видео", isLocked = true)
                )
            )
        )

        renderBlocks(blocks)
    }

    private fun renderBlocks(blocks: List<OnboardingBlock>) {
        llBlocksContainer.removeAllViews()
        blocks.forEach { block ->
            val blockView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_onboarding_block, llBlocksContainer, false)

            val tvNumber = blockView.findViewById<TextView>(R.id.tvBlockNumber)
            val tvTitle = blockView.findViewById<TextView>(R.id.tvBlockTitle)
            val tvProgress = blockView.findViewById<TextView>(R.id.tvBlockProgress)
            val ivChevron = blockView.findViewById<ImageView>(R.id.ivChevron)
            val rlHeader = blockView.findViewById<RelativeLayout>(R.id.rlBlockHeader)
            val llLessonsContainer = blockView.findViewById<LinearLayout>(R.id.llLessonsContainer)

            tvNumber.text = block.number.toString()
            tvTitle.text = block.title
            tvProgress.text = getString(R.string.onboarding_lessons_count, 0, block.lessons.size)

            // Render lessons
            block.lessons.forEach { lesson ->
                val lessonView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_onboarding_lesson, llLessonsContainer, false)
                
                lessonView.findViewById<TextView>(R.id.tvLessonTitle).text = lesson.title
                lessonView.findViewById<TextView>(R.id.tvLessonDuration).text = lesson.duration
                lessonView.findViewById<TextView>(R.id.tvLessonType).text = lesson.type
                
                if (lesson.isLocked) {
                    lessonView.alpha = 0.5f
                    lessonView.findViewById<ImageView>(R.id.ivLessonStatus).setImageResource(R.drawable.ic_lock_closed)
                }

                llLessonsContainer.addView(lessonView)
            }

            // Always add final test block as requested
            val finalTestView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_onboarding_final_test, llLessonsContainer, false)
            llLessonsContainer.addView(finalTestView)

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

    data class OnboardingBlock(
        val number: Int,
        val title: String,
        val lessons: List<Lesson>
    )

    data class Lesson(
        val title: String,
        val duration: String,
        val type: String,
        val isLocked: Boolean = false
    )
}