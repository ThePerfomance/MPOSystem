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
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.groupprojectfirsttry.BuildConfig
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.ThemeManager
import com.example.groupprojectfirsttry.adapters.SubjectSelectionAdapter
import com.example.groupprojectfirsttry.api.*
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.Block
import com.example.groupprojectfirsttry.simpleClasses.Lesson
import com.example.groupprojectfirsttry.simpleClasses.Subject
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.UUID

class HomeFragment : Fragment() {
    
    private var llHomeBlocksContainer: LinearLayout? = null
    private val apiService = ApiClient.apiService
    private lateinit var tokenManager: TokenManager
    
    private lateinit var shimmerHome: ShimmerFrameLayout
    private lateinit var nsvHomeContent: View
    private lateinit var shimmerTrainerBadge: ShimmerFrameLayout
    private lateinit var swipeRefreshHome: SwipeRefreshLayout
    private var llSubjectSelector: LinearLayout? = null
    private var ivSubjectChevron: ImageView? = null
    private var tvWelcome: TextView? = null

    private var subjectsList: List<Subject> = emptyList()
    private var selectedSubject: Subject? = null
    private var finishedTestIds: Set<Int> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tokenManager = ApiClient.getTokenManager() ?: TokenManager(requireContext())
        
        shimmerHome = view.findViewById(R.id.shimmer_home)
        nsvHomeContent = view.findViewById(R.id.nsvHomeContent)
        shimmerTrainerBadge = view.findViewById(R.id.shimmerTrainerBadge)
        swipeRefreshHome = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshHome)
        llSubjectSelector = view.findViewById(R.id.llSubjectSelector)
        ivSubjectChevron = view.findViewById(R.id.ivSubjectChevron)
        tvWelcome = view.findViewById(R.id.tvWelcomeUser)
        
        setupSwipeRefresh()
        setupHome(view, isRefresh = false)
    }

    private fun setupSwipeRefresh() {
        swipeRefreshHome.setColorSchemeResources(R.color.AccentColor)
        swipeRefreshHome.setOnRefreshListener {
            setupHome(requireView(), isRefresh = true)
        }
    }

    private fun startLoading() {
        shimmerHome.visibility = View.VISIBLE
        shimmerHome.startShimmer()
        nsvHomeContent.visibility = View.GONE
    }

    private fun stopLoading() {
        shimmerHome.stopShimmer()
        shimmerHome.visibility = View.GONE
        nsvHomeContent.visibility = View.VISIBLE
    }

    private fun setupHome(view: View, isRefresh: Boolean = false) {
        val userProvider = activity as? UserProvider
        val user = userProvider?.getUser()
        val userId = user?.id ?: return
        
        if (isAdded && tvWelcome != null) {
            val name = when {
                !user.firstname.isNullOrBlank() -> user.firstname
                !user.username.isNullOrBlank() -> user.username
                else -> ""
            }
            tvWelcome?.text = if (name.isNotBlank()) {
                getString(R.string.welcome_user_format, name)
            } else {
                "Привет! 👋"
            }
        }
        
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
        llHomeBlocksContainer = view.findViewById(R.id.llHomeBlocksContainer)

        val tvAvgScoreValue = view.findViewById<TextView>(R.id.tvAvgScoreValue)
        val tvTestsPassedCount = view.findViewById<TextView>(R.id.tvTestsPassedCount)
        val tvTrainerBadge = view.findViewById<TextView>(R.id.tvTrainerBadge)
        val btnStartTrainer = view.findViewById<MaterialButton>(R.id.btnStartTrainerHome)
        val cvTrainer = view.findViewById<View>(R.id.cvTrainer)

        val cvRecommendations = view.findViewById<View>(R.id.cvRecommendations)
        val tvRecBadge = view.findViewById<TextView>(R.id.tvRecommendationsBadge)
        val btnViewRec = view.findViewById<MaterialButton>(R.id.btnViewRecommendations)
        
        val btnViewTestHistory = view.findViewById<MaterialButton>(R.id.btnViewTestHistory)

        val isTrainerEnabled = ThemeManager.isTrainerEnabled(requireContext())
        cvTrainer?.isVisible = isTrainerEnabled
        
        val isRecommendationsEnabled = BuildConfig.FLAVOR != "impuls"
        cvRecommendations?.isVisible = isRecommendationsEnabled

        btnStartTrainer?.setOnClickListener {
            (requireActivity() as? SecondActivityWithBottomNavMenu)
                ?.replaceFragment(TrainingListFragment(), null)
        }

        btnViewRec?.setOnClickListener {
            (requireActivity() as? SecondActivityWithBottomNavMenu)
                ?.replaceFragment(RecommendationsFragment(), null)
        }
        
        btnViewTestHistory?.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable("user", user)
            }
            (requireActivity() as? SecondActivityWithBottomNavMenu)
                ?.replaceFragment(TestStudentResult(), bundle)
        }

        llSubjectSelector?.setOnClickListener {
            if (subjectsList.size > 1) {
                showSubjectSelectionDialog()
            }
        }

        if (!isRefresh) {
            startLoading()
        }
        shimmerTrainerBadge.startShimmer()

        lifecycleScope.launch {
            try {
                // 1. Статистика
                try {
                    val userResults = apiService.getUserTestResults(userId)
                    if (isAdded) {
                        finishedTestIds = userResults.map { it.test_id }.toSet()
                        
                        val totalEarned = userResults.sumOf { it.earnedPoints }
                        val totalPossible = userResults.sumOf { it.totalPoints }.coerceAtLeast(1)
                        val avgScore = (totalEarned.toDouble() / totalPossible * 100).toInt()
                        
                        tvAvgScoreValue?.text = "$avgScore%"
                        tvTestsPassedCount?.text = "Пройдено тестов: ${userResults.size}"
                    }
                } catch (e: Exception) { Log.e("API_ERROR", "Stats load failed") }

                // 2. ML Рекомендации
                if (isRecommendationsEnabled) {
                    try {
                        val response = apiService.getPersonalizedRecommendations(userId)
                        if (response.isSuccessful && isAdded) {
                            val recs = response.body()?.recommendations ?: emptyList()
                            tvRecBadge?.text = if (recs.isNotEmpty()) "${recs.size} рекомендации" else "Все отлично"
                            tvRecBadge?.setBackgroundResource(if (recs.isNotEmpty()) R.drawable.bg_badge_orange else R.drawable.bg_badge_purple)
                        }
                    } catch (e: Exception) { }
                }

                // 3. Группы и Предметы
                val groups = apiService.getUserGroups(userId)
                
                if (groups.isNotEmpty()) {
                    val groupId = groups[0].id
                    subjectsList = apiService.getGroupSubjects(groupId)
                    
                    if (subjectsList.isNotEmpty()) {
                        val savedId = tokenManager.getSelectedSubjectId()
                        selectedSubject = subjectsList.find { it.id == savedId } ?: subjectsList[0]
                        selectedSubject?.let { tokenManager.saveSelectedSubjectId(it.id) }
                        
                        updateSelectedSubjectUI()
                        ivSubjectChevron?.isVisible = subjectsList.size > 1
                    } else {
                        tvSubtitle?.text = "Предметы не назначены"
                        ivSubjectChevron?.isVisible = false
                    }
                } else {
                    tvSubtitle?.text = "Группа не найдена"
                    ivSubjectChevron?.isVisible = false
                }

                // 4. Тренажер (По всем предметам)
                loadAllTrainerData(userId)

            } catch (e: Exception) {
                Log.e("API_ERROR", "Critical error in setupHome", e)
                handleNetworkError(e)
            } finally {
                if (isAdded) {
                    if (!isRefresh) stopLoading()
                    swipeRefreshHome.isRefreshing = false
                    stopTrainerShimmer()
                }
            }
        }
    }

    private fun stopTrainerShimmer() {
        shimmerTrainerBadge.stopShimmer()
        shimmerTrainerBadge.setShimmer(null)
    }

    private fun updateSelectedSubjectUI() {
        val subject = selectedSubject ?: return
        view?.findViewById<TextView>(R.id.tvSubtitle)?.text = subject.name
        
        lifecycleScope.launch {
            try {
                llHomeBlocksContainer?.removeAllViews()
                val blocks = apiService.getBlocksBySubject(subject.id)
                val blocksWithLessons = blocks.map { block ->
                    async {
                        try {
                            block to apiService.getLessonsByBlock(block.id)
                        } catch (e: Exception) {
                            block to emptyList<Lesson>()
                        }
                    }
                }.awaitAll()

                if (isAdded) renderHomeBlocks(blocksWithLessons, finishedTestIds)
            } catch (e: Exception) {
                Log.e("API_ERROR", "Failed to load blocks: ${e.message}")
            }
        }
    }

    private suspend fun loadAllTrainerData(userId: UUID) {
        try {
            val sessions = apiService.getTrainingSessions(userId)
            val total = sessions.filter { it.status != "completed" }
                .sumOf { session ->
                    session.questions?.count { q -> (q.status == "pending" || q.status == "wrong") } ?: 0
                }
            
            if (isAdded) {
                val tvTrainerBadge = view?.findViewById<TextView>(R.id.tvTrainerBadge)
                val btnStartTrainer = view?.findViewById<MaterialButton>(R.id.btnStartTrainerHome)
                
                tvTrainerBadge?.text = if (total > 0) "$total вопросов" else "Вопросы отсутствуют"
                btnStartTrainer?.isVisible = total > 0
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Trainer load failed: ${e.message}")
        } finally {
            stopTrainerShimmer()
        }
    }

    private fun showSubjectSelectionDialog() {
        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)
        val bottomSheetView = layoutInflater.inflate(R.layout.layout_subject_selection, null)
        
        val rv = bottomSheetView.findViewById<RecyclerView>(R.id.rvSubjectSelection)
        rv.layoutManager = LinearLayoutManager(requireContext())
        
        val adapter = SubjectSelectionAdapter(subjectsList, selectedSubject?.id) { newSubject ->
            if (newSubject.id != selectedSubject?.id) {
                selectedSubject = newSubject
                tokenManager.saveSelectedSubjectId(newSubject.id)
                updateSelectedSubjectUI()
            }
            dialog.dismiss()
        }
        rv.adapter = adapter
        
        dialog.setContentView(bottomSheetView)
        dialog.show()
    }

    private fun handleNetworkError(e: Exception) {
        if (!isAdded) return
        val message = when (e) {
            is ApiException -> "Ошибка API: ${e.code}"
            else -> "Ошибка загрузки данных"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun renderHomeBlocks(data: List<Pair<Block, List<Lesson>>>, finishedTestIds: Set<Int>) {
        if (!isAdded) return
        val container = llHomeBlocksContainer ?: return
        container.removeAllViews()
        
        data.sortedBy { it.first.position }.forEach { (block, lessons) ->
            val blockView = LayoutInflater.from(requireContext()).inflate(R.layout.item_home_block, container, false)
            blockView.findViewById<TextView>(R.id.tvBlockTitle).text = block.title
            
            val total = block.lessonsCount
            val finished = lessons.count { it.test != null && finishedTestIds.contains(it.test) }
            val percent = if (total > 0) (finished * 100) / total else 0
            
            blockView.findViewById<TextView>(R.id.tvBlockProgressText).text = "$finished / $total"
            blockView.findViewById<ProgressBar>(R.id.pbBlock).progress = percent

            val llLessonsContainer = blockView.findViewById<LinearLayout>(R.id.llLessonsContainerHome)
            renderLessons(llLessonsContainer, lessons, block.title, finishedTestIds)

            blockView.findViewById<View>(R.id.rlBlockHeaderHome).setOnClickListener {
                val willBeVisible = !llLessonsContainer.isVisible
                TransitionManager.beginDelayedTransition(container, AutoTransition())
                llLessonsContainer.isVisible = willBeVisible
                blockView.findViewById<ImageView>(R.id.ivChevronHome).rotation = if (willBeVisible) 180f else 0f
            }
            container.addView(blockView)
        }
    }

    private fun renderLessons(container: LinearLayout, lessons: List<Lesson>, blockTitle: String, finishedTestIds: Set<Int>) {
        if (!isAdded) return
        container.removeAllViews()
        lessons.sortedBy { it.position }.forEach { lesson ->
            val lessonView = LayoutInflater.from(requireContext()).inflate(R.layout.item_onboarding_lesson, container, false)
            lessonView.findViewById<TextView>(R.id.tvLessonTitle).text = lesson.title
            
            val type = if (!lesson.video?.finalLink.isNullOrEmpty()) "Видео" else "Чтение"
            lessonView.findViewById<TextView>(R.id.tvLessonType).text = type
            
            val ivStatus = lessonView.findViewById<ImageView>(R.id.ivLessonStatus)
            val isFinished = lesson.test != null && finishedTestIds.contains(lesson.test)
            ivStatus.setImageResource(if (isFinished) R.drawable.ic_circle_filled else R.drawable.ic_circle_outline)
            ivStatus.setColorFilter(if (isFinished) Color.BLACK else Color.GRAY)
            
            if (lesson.isPublished) {
                lessonView.setOnClickListener {
                    val detailFragment = LessonDetailFragment.newInstance(lesson, blockTitle)
                    (activity as? SecondActivityWithBottomNavMenu)?.replaceFragment(detailFragment, detailFragment.arguments)
                }
            } else {
                lessonView.alpha = 0.5f
            }
            container.addView(lessonView)
        }
    }
}
