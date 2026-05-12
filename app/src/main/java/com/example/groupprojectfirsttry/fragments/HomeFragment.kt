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
        
        llHomeBlocksContainer = view.findViewById(R.id.llHomeBlocksContainer)
        
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
        val tvAvgScoreValue = view.findViewById<TextView>(R.id.tvAvgScoreValue)
        val tvTestsPassedCount = view.findViewById<TextView>(R.id.tvTestsPassedCount)
        val btnStartTrainer = view.findViewById<MaterialButton>(R.id.btnStartTrainerHome)
        val cvTrainer = view.findViewById<View>(R.id.cvTrainer)
        val btnViewTestHistory = view.findViewById<MaterialButton>(R.id.btnViewTestHistory)

        // Trainer settings
        cvTrainer?.isVisible = ThemeManager.isTrainerEnabled(requireContext())
        btnStartTrainer?.text = if (ThemeManager.isAdaptiveTrainerEnabled(requireContext())) {
            "Запустить адаптивный тренажёр"
        } else {
            "Начать тренировку"
        }

        btnStartTrainer?.setOnClickListener {
            if (ThemeManager.isAdaptiveTrainerEnabled(requireContext())) {
                startAdaptiveTraining()
            } else {
                (requireActivity() as? SecondActivityWithBottomNavMenu)
                    ?.replaceFragment(TrainingListFragment(), null)
            }
        }
        
        btnViewTestHistory?.setOnClickListener {
            val bundle = Bundle().apply { putParcelable("user", user) }
            (requireActivity() as? SecondActivityWithBottomNavMenu)
                ?.replaceFragment(TestStudentResult(), bundle)
        }

        llSubjectSelector?.setOnClickListener {
            if (subjectsList.size > 1) showSubjectSelectionDialog()
        }

        if (!isRefresh) startLoading()
        shimmerTrainerBadge.startShimmer()

        lifecycleScope.launch {
            try {
                // 1. Статистика (Ждем её выполнения перед обновлением UI)
                val userResults = try {
                    apiService.getUserTestResults(userId)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Stats load failed")
                    emptyList()
                }
                
                finishedTestIds = userResults.map { it.test_id }.toSet()
                
                if (isAdded) {
                    val totalEarned = userResults.sumOf { it.earnedPoints }
                    val totalPossible = userResults.sumOf { it.totalPoints }.coerceAtLeast(1)
                    val avgScore = (totalEarned.toDouble() / totalPossible * 100).toInt()
                    
                    tvAvgScoreValue?.text = "$avgScore%"
                    tvTestsPassedCount?.text = "Пройдено тестов: ${userResults.size}"
                }

                // 2. Группы и Предметы
                val groups = apiService.getUserGroups(userId)
                if (groups.isNotEmpty()) {
                    subjectsList = apiService.getGroupSubjects(groups[0].id)
                    if (subjectsList.isNotEmpty()) {
                        val savedId = tokenManager.getSelectedSubjectId()
                        selectedSubject = subjectsList.find { it.id == savedId } ?: subjectsList[0]
                        selectedSubject?.let { 
                            tokenManager.saveSelectedSubjectId(it.id)
                            tvSubtitle?.text = it.name
                            updateSelectedSubjectUI()
                        }
                        ivSubjectChevron?.isVisible = subjectsList.size > 1
                    } else {
                        tvSubtitle?.text = "Предметы не назначены"
                    }
                } else {
                    tvSubtitle?.text = "Группа не найдена"
                }

                // 3. Тренажер
                loadAllTrainerData(userId)

            } catch (e: Exception) {
                Log.e("HomeFragment", "Critical error in setupHome", e)
                handleNetworkError(e)
            } finally {
                if (isAdded) {
                    if (!isRefresh) stopLoading()
                    swipeRefreshHome.isRefreshing = false
                }
            }
        }
    }

    private fun startAdaptiveTraining() {
        val btnStartTrainer = view?.findViewById<MaterialButton>(R.id.btnStartTrainerHome)
        
        btnStartTrainer?.isEnabled = false
        btnStartTrainer?.text = "Подбор вопросов..."
        
        lifecycleScope.launch {
            try {
                // Используем объект запроса AdaptiveTrainingRequest вместо MutableMap
                val request = AdaptiveTrainingRequest(
                    lessonId = null, // В главном меню запускаем глобальный тренажер
                    onlyPassed = true,
                    excludeCorrect = true
                )

                val response = ApiClient.apiService.createAdaptiveTrainingSession(request)
                if (response.isSuccessful && isAdded) {
                    val session = response.body()?.session
                    if (session != null && !session.questions.isNullOrEmpty()) {
                        val bundle = Bundle().apply {
                            putParcelable("session", session)
                            putBoolean("is_adaptive", true)
                        }
                        (requireActivity() as? SecondActivityWithBottomNavMenu)
                            ?.replaceFragment(TrainingFragment(), bundle)
                    } else {
                        Toast.makeText(requireContext(), "Нет доступных вопросов", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show()
            } finally {
                if (isAdded) {
                    btnStartTrainer?.isEnabled = true
                    btnStartTrainer?.text = "Запустить адаптивный тренажёр"
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
                val blocks = apiService.getBlocksBySubject(subject.id).sortedBy { it.position }
                
                // Параллельная загрузка уроков для всех блоков
                val blocksWithLessons = blocks.map { block ->
                    async {
                        try {
                            val lessons = apiService.getLessonsByBlock(block.id)
                            block to lessons.sortedBy { it.position }
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Failed to load lessons for block ${block.id}")
                            block to emptyList<Lesson>()
                        }
                    }
                }.awaitAll()

                if (isAdded) renderHomeBlocks(blocksWithLessons, finishedTestIds)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to load subject content")
            }
        }
    }

    private suspend fun loadAllTrainerData(userId: UUID) {
        try {
            val sessions = apiService.getTrainingSessions(userId)
            val total = sessions.sumOf { session ->
                session.questions?.count { it.status != "correct" } ?: 0
            }
            
            if (isAdded) {
                val tvTrainerBadge = view?.findViewById<TextView>(R.id.tvTrainerBadge)
                if (ThemeManager.isAdaptiveTrainerEnabled(requireContext())) {
                    tvTrainerBadge?.text = if (total > 0) "Ошибок: $total (Тренажер)" else "Персональный подбор"
                } else {
                    tvTrainerBadge?.text = if (total > 0) "$total вопросов" else "Вопросы отсутствуют"
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Trainer data fetch failed")
        } finally {
            stopTrainerShimmer()
        }
    }

    private fun renderHomeBlocks(data: List<Pair<Block, List<Lesson>>>, finishedIds: Set<Int>) {
        if (!isAdded) return
        val container = llHomeBlocksContainer ?: return
        container.removeAllViews()
        
        data.forEach { (block, lessons) ->
            val blockView = layoutInflater.inflate(R.layout.item_home_block, container, false)
            
            val tvTitle = blockView.findViewById<TextView>(R.id.tvBlockTitle)
            val tvProgress = blockView.findViewById<TextView>(R.id.tvBlockProgressText)
            val pbBlock = blockView.findViewById<ProgressBar>(R.id.pbBlock)
            val llLessons = blockView.findViewById<LinearLayout>(R.id.llLessonsContainerHome)
            val ivChevron = blockView.findViewById<ImageView>(R.id.ivChevronHome)
            val rlHeader = blockView.findViewById<View>(R.id.rlBlockHeaderHome)

            tvTitle.text = block.title
            val total = lessons.size
            val completed = lessons.count { it.test != null && finishedIds.contains(it.test) }
            
            tvProgress.text = "$completed / $total"
            pbBlock.max = if (total > 0) total else 1
            pbBlock.progress = completed

            // Отрисовка уроков
            if (lessons.isNotEmpty()) {
                lessons.forEach { lesson ->
                    val lessonView = layoutInflater.inflate(R.layout.item_onboarding_lesson, llLessons, false)
                    lessonView.findViewById<TextView>(R.id.tvLessonTitle).text = lesson.title
                    lessonView.findViewById<TextView>(R.id.tvLessonDuration).text = "${lesson.duration / 60} мин"
                    lessonView.findViewById<TextView>(R.id.tvLessonType).text = if (!lesson.video?.finalLink.isNullOrEmpty()) "Видео" else "Чтение"

                    val ivStatus = lessonView.findViewById<ImageView>(R.id.ivLessonStatus)
                    val isDone = lesson.test != null && finishedIds.contains(lesson.test)
                    
                    if (isDone) {
                        ivStatus.setImageResource(R.drawable.ic_check_circle_green)
                        ivStatus.setColorFilter(Color.parseColor("#4CAF50"))
                    } else {
                        ivStatus.setImageResource(R.drawable.ic_circle_outline)
                        ivStatus.setColorFilter(Color.parseColor("#BDBDBD"))
                    }

                    lessonView.setOnClickListener {
                        val bundle = Bundle().apply {
                            putParcelable("lesson", lesson)
                            putParcelable("subject", selectedSubject)
                        }
                        (requireActivity() as? SecondActivityWithBottomNavMenu)
                            ?.replaceFragment(LessonDetailFragment(), bundle)
                    }
                    llLessons.addView(lessonView)
                }
            } else {
                val emptyView = TextView(requireContext()).apply {
                    text = "В этом блоке пока нет уроков"
                    setPadding(64, 32, 64, 32)
                    setTextColor(Color.GRAY)
                }
                llLessons.addView(emptyView)
            }

            // Логика раскрытия - принудительное переключение видимости
            rlHeader.setOnClickListener {
                val willBeVisible = llLessons.visibility != View.VISIBLE
                
                // Используем framework TransitionManager для анимации
                TransitionManager.beginDelayedTransition(container, AutoTransition())
                
                llLessons.visibility = if (willBeVisible) View.VISIBLE else View.GONE
                
                // Анимация вращения стрелки
                ivChevron.animate()
                    .rotation(if (willBeVisible) 180f else 0f)
                    .setDuration(250)
                    .start()
            }
            
            container.addView(blockView)
        }
    }

    private fun showSubjectSelectionDialog() {
        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.layout_subject_selection, null)
        
        val rv = view.findViewById<RecyclerView>(R.id.rvSubjectSelection)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = SubjectSelectionAdapter(subjectsList, selectedSubject?.id) { subject ->
            selectedSubject = subject
            tokenManager.saveSelectedSubjectId(subject.id)
            updateSelectedSubjectUI()
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun handleNetworkError(e: Exception) {
        if (!isAdded) return
        Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
    }
}
