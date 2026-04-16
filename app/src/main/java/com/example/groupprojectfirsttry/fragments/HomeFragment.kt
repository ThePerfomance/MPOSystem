package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.BuildConfig
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.Block
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.UUID

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

                    // 2. Load training sessions info
                    val sessions = apiService.getTrainingSessions(userId)
                    val totalUnresolved = sessions
                        .filter { it.status != "completed" }
                        .sumOf { session ->
                            session.questions?.count { it.status == "pending" || it.status == "wrong" } ?: 0
                        }
                    
                    tvTrainerBadge.text = "$totalUnresolved вопросов"
                    btnStartTrainer.isEnabled = totalUnresolved > 0

                    // 3. Load subjects and blocks
                    val subjects = apiService.getSubjects()
                    if (subjects.isNotEmpty()) {
                        val subject = if (subjects.size > 2) subjects[2] else subjects[0]
                        tvSubtitle?.text = subject.name
                        
                        val blocks = apiService.getBlocksBySubject(subject.id)
                        
                        val blocksWithFinishedCount = blocks.map { block ->
                            async {
                                try {
                                    val lessons = apiService.getLessonsByBlock(block.id)
                                    val finishedInBlock = lessons.count { it.test != null && finishedTestIds.contains(it.test) }
                                    block to finishedInBlock
                                } catch (e: Exception) {
                                    block to 0
                                }
                            }
                        }.awaitAll()

                        val blockProgressMap = blocksWithFinishedCount.toMap()
                        renderHomeBlocks(blockProgressMap)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading data", e)
            }
        }
    }

    private fun renderHomeBlocks(progressMap: Map<Block, Int>) {
        if (!isAdded) return
        val container = llHomeBlocksContainer ?: return
        container.removeAllViews()
        
        progressMap.keys.sortedBy { it.position }.forEach { block ->
            val blockView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_home_block, container, false)

            blockView.findViewById<TextView>(R.id.tvBlockTitle).text = block.title
            
            val total = block.lessonsCount
            val finished = progressMap[block] ?: 0
            val percent = if (total > 0) (finished * 100) / total else 0
            
            blockView.findViewById<TextView>(R.id.tvBlockProgressText).text = "$finished / $total"
            blockView.findViewById<ProgressBar>(R.id.pbBlock).progress = percent

            container.addView(blockView)
        }
    }
}
