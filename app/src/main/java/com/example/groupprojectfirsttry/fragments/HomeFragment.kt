package com.example.groupprojectfirsttry.fragments

import android.graphics.Color
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
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.BuildConfig
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.ThemeManager
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.Block
import com.example.groupprojectfirsttry.simpleClasses.Lesson
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    
    private var llHomeBlocksContainer: LinearLayout? = null
    private val apiService = ApiClient.apiService

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
        val userId = user?.id ?: return
        
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcomeUser)
        if (isAdded) {
            tvWelcome.text = getString(R.string.welcome_user_format, user.firstname)
        }
        
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
        llHomeBlocksContainer = view.findViewById(R.id.llHomeBlocksContainer)

        // UI components for new sections
        val tvAvgScoreValue = view.findViewById<TextView>(R.id.tvAvgScoreValue)
        val tvTestsPassedCount = view.findViewById<TextView>(R.id.tvTestsPassedCount)
        val tvTrainerBadge = view.findViewById<TextView>(R.id.tvTrainerBadge)
        val btnStartTrainer = view.findViewById<MaterialButton>(R.id.btnStartTrainerHome)
        val cvTrainer = view.findViewById<View>(R.id.cvTrainer)

        // Проверка включен ли тренажер в настройках
        val isTrainerEnabled = ThemeManager.isTrainerEnabled(requireContext())
        cvTrainer?.isVisible = isTrainerEnabled

        btnStartTrainer.setOnClickListener {
            (requireActivity() as? SecondActivityWithBottomNavMenu)
                ?.replaceFragment(TrainingListFragment(), null)
        }

        lifecycleScope.launch {
            try {
                // 1. Load test statistics
                val userResults = apiService.getUserTestResults(userId)
                if (isAdded) {
                    val finishedTestIds = userResults.map { it.test_id }.toSet()
                    val avgScore = if (userResults.isNotEmpty()) userResults.map { it.score }.average().toInt() else 0
                    
                    tvAvgScoreValue.text = "$avgScore%"
                    tvTestsPassedCount.text = "Пройдено тестов: ${userResults.size}"

                    // 2. Load training sessions info if trainer is enabled
                    if (isTrainerEnabled) {
                        val sessions = apiService.getTrainingSessions(userId)
                        val totalUnresolved = sessions
                            .filter { it.status != "completed" }
                            .sumOf { session ->
                                session.questions?.count { it.status == "pending" || it.status == "wrong" } ?: 0
                            }
                        
                        if (totalUnresolved > 0) {
                            tvTrainerBadge.text = "$totalUnresolved вопросов"
                            btnStartTrainer.visibility = View.VISIBLE
                        } else {
                            tvTrainerBadge.text = "Вопросы отсутствуют"
                            btnStartTrainer.visibility = View.GONE
                        }
                    }

                    // 3. Load subjects and blocks
                    val subjects = apiService.getSubjects()
                    if (subjects.isNotEmpty()) {
                        val subject = if (subjects.size > 2) subjects[2] else subjects[0]
                        tvSubtitle?.text = subject.name
                        
                        val blocks = apiService.getBlocksBySubject(subject.id)
                        
                        val blocksWithLessons = blocks.map { block ->
                            async {
                                try {
                                    val lessons = apiService.getLessonsByBlock(block.id)
                                    block to lessons
                                } catch (e: Exception) {
                                    block to emptyList<Lesson>()
                                }
                            }
                        }.awaitAll()

                        renderHomeBlocks(blocksWithLessons, finishedTestIds)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading data", e)
            }
        }
    }

    private fun renderHomeBlocks(data: List<Pair<Block, List<Lesson>>>, finishedTestIds: Set<Int>) {
        if (!isAdded) return
        val container = llHomeBlocksContainer ?: return
        container.removeAllViews()
        
        data.sortedBy { it.first.position }.forEach { (block, lessons) ->
            val blockView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_home_block, container, false)

            blockView.findViewById<TextView>(R.id.tvBlockTitle).text = block.title
            
            val total = block.lessonsCount
            val finished = lessons.count { it.test != null && finishedTestIds.contains(it.test) }
            val percent = if (total > 0) (finished * 100) / total else 0
            
            blockView.findViewById<TextView>(R.id.tvBlockProgressText).text = "$finished / $total"
            blockView.findViewById<ProgressBar>(R.id.pbBlock).progress = percent

            val ivChevron = blockView.findViewById<ImageView>(R.id.ivChevronHome)
            val llLessonsContainer = blockView.findViewById<LinearLayout>(R.id.llLessonsContainerHome)
            val rlHeader = blockView.findViewById<View>(R.id.rlBlockHeaderHome)

            renderLessons(llLessonsContainer, lessons, block.title, finishedTestIds)

            rlHeader.setOnClickListener {
                val willBeVisible = !llLessonsContainer.isVisible
                TransitionManager.beginDelayedTransition(container, AutoTransition())
                llLessonsContainer.isVisible = willBeVisible
                ivChevron.animate()
                    .rotation(if (willBeVisible) 180f else 0f)
                    .setDuration(300)
                    .start()
            }

            container.addView(blockView)
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
            
            val type = if (!lesson.video?.finalLink.isNullOrEmpty()) "Видео" else "Чтение"
            lessonView.findViewById<TextView>(R.id.tvLessonType).text = type
            
            val ivStatus = lessonView.findViewById<ImageView>(R.id.ivLessonStatus)
            val isFinished = lesson.test != null && finishedTestIds.contains(lesson.test)

            if (isFinished) {
                ivStatus.setImageResource(R.drawable.ic_circle_filled)
                ivStatus.setColorFilter(Color.BLACK)
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
}
