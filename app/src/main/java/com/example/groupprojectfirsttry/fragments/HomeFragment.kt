package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.BuildConfig
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.interfaces.UserProvider

class HomeFragment : Fragment() {
    
    private var llHomeBlocksContainer: LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutRes = if (BuildConfig.FLAVOR == "impuls") {
            R.layout.fragment_home_impuls
        } else {
            R.layout.fragment_home
        }
        return inflater.inflate(layoutRes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (BuildConfig.FLAVOR == "impuls") {
            setupImpulsHome(view)
        }
    }

    private fun setupImpulsHome(view: View) {
        val userProvider = activity as? UserProvider
        val user = userProvider?.getUser()
        
        // Welcome and Subtitle logic
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcomeUser)
        if (user != null) {
            tvWelcome.text = "Привет, ${user.firstname}! 👋"
        }
        
        // Mock Progress Data
        val totalLessons = 5
        val finishedLessons = 0
        val percent = if (totalLessons > 0) (finishedLessons * 100) / totalLessons else 0
        
        view.findViewById<TextView>(R.id.tvTotalProgressCount).text = "$finishedLessons / $totalLessons"
        view.findViewById<ProgressBar>(R.id.pbTotal).progress = percent
        view.findViewById<TextView>(R.id.tvTotalPercent).text = "$percent%"
        
        view.findViewById<TextView>(R.id.tvFinishedCount).text = finishedLessons.toString()
        view.findViewById<TextView>(R.id.tvLeftCount).text = (totalLessons - finishedLessons).toString()

        // Dynamic Blocks logic
        llHomeBlocksContainer = view.findViewById(R.id.llHomeBlocksContainer)
        
        // Use the same structure as Onboarding
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
        
        renderHomeBlocks(blocks)
    }

    private fun renderHomeBlocks(blocks: List<OnboardingBlock>) {
        val container = llHomeBlocksContainer ?: return
        container.removeAllViews()
        
        blocks.forEach { block ->
            val blockView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_home_block, container, false)

            blockView.findViewById<TextView>(R.id.tvBlockTitle).text = block.title
            
            val total = block.lessons.size
            val finished = 0 // Mock finished
            val percent = if (total > 0) (finished * 100) / total else 0
            
            blockView.findViewById<TextView>(R.id.tvBlockProgressText).text = "$finished / $total"
            blockView.findViewById<ProgressBar>(R.id.pbBlock).progress = percent

            container.addView(blockView)
        }
    }

    // Reuse or import these data classes
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