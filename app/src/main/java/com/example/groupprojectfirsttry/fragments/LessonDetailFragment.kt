package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.R

class LessonDetailFragment : Fragment(R.layout.fragment_lesson_detail) {

    private var currentTab = 0 // 0: Video, 1: Summary, 2: Test

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lessonTitle = arguments?.getString("lesson_title") ?: "Урок"
        val blockTitle = arguments?.getString("block_title") ?: "Блок"

        view.findViewById<TextView>(R.id.tvLessonTitleDetail).text = lessonTitle
        view.findViewById<TextView>(R.id.tvBlockTitleDetail).text = blockTitle

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        setupTabs(view)
    }

    private fun setupTabs(view: View) {
        val tabVideo = view.findViewById<LinearLayout>(R.id.tabVideo)
        val tabSummary = view.findViewById<LinearLayout>(R.id.tabSummary)
        val tabTest = view.findViewById<LinearLayout>(R.id.tabTest)

        val contentVideo = view.findViewById<View>(R.id.cvVideoContent)
        val contentSummary = view.findViewById<View>(R.id.tvSummaryContent)
        val contentTest = view.findViewById<View>(R.id.tvTestContent)

        val tabs = listOf(tabVideo, tabSummary, tabTest)
        val contents = listOf(contentVideo, contentSummary, contentTest)

        tabs.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                updateTabs(index, tabs, contents)
            }
        }
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
        fun newInstance(lessonTitle: String, blockTitle: String): LessonDetailFragment {
            return LessonDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("lesson_title", lessonTitle)
                    putString("block_title", blockTitle)
                }
            }
        }
    }
}