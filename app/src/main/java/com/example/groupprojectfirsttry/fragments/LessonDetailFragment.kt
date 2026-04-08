package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.Lesson

class LessonDetailFragment : Fragment(R.layout.fragment_lesson_detail) {

    private var currentTab = 1 // Default to Summary (index 1)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lesson = arguments?.getParcelable<Lesson>("lesson")
        val blockTitle = arguments?.getString("block_title") ?: "Блок"

        val lessonTitle = lesson?.title ?: "Урок"
        view.findViewById<TextView>(R.id.tvLessonTitleDetail).text = lessonTitle
        view.findViewById<TextView>(R.id.tvBlockTitleDetail).text = blockTitle

        // Set summary content from DB
        val tvSummaryContent = view.findViewById<TextView>(R.id.tvSummaryContent)
        tvSummaryContent.text = lesson?.summary ?: "Нет описания"

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        setupTabs(view, lesson)
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
