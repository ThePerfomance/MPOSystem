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
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.Block
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
        
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcomeUser)
        if (user != null) {
            tvWelcome.text = getString(R.string.welcome_user_format, user.firstname)
        }
        
        llHomeBlocksContainer = view.findViewById(R.id.llHomeBlocksContainer)

        lifecycleScope.launch {
            try {
                val subjects = apiService.getSubjects()
                if (subjects.isNotEmpty()) {
                    val firstSubject = subjects[0]
                    val blocks = apiService.getBlocksBySubject(firstSubject.id)
                    
                    updateOverallProgress(view, blocks)
                    renderHomeBlocks(blocks)
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading data", e)
            }
        }
    }

    private fun updateOverallProgress(view: View, blocks: List<Block>) {
        val totalLessons = blocks.sumOf { it.lessonsCount }
        val finishedLessons = 0 // Здесь должна быть логика получения прогресса пользователя
        val percent = if (totalLessons > 0) (finishedLessons * 100) / totalLessons else 0
        
        view.findViewById<TextView>(R.id.tvTotalProgressCount).text = "$finishedLessons / $totalLessons"
        view.findViewById<ProgressBar>(R.id.pbTotal).progress = percent
        view.findViewById<TextView>(R.id.tvTotalPercent).text = "$percent%"
        
        view.findViewById<TextView>(R.id.tvFinishedCount).text = finishedLessons.toString()
        view.findViewById<TextView>(R.id.tvLeftCount).text = (totalLessons - finishedLessons).toString()
    }

    private fun renderHomeBlocks(blocks: List<Block>) {
        val container = llHomeBlocksContainer ?: return
        container.removeAllViews()
        
        blocks.sortedBy { it.position }.forEach { block ->
            val blockView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_home_block, container, false)

            blockView.findViewById<TextView>(R.id.tvBlockTitle).text = block.title
            
            val total = block.lessonsCount
            val finished = 0 
            val percent = if (total > 0) (finished * 100) / total else 0
            
            blockView.findViewById<TextView>(R.id.tvBlockProgressText).text = "$finished / $total"
            blockView.findViewById<ProgressBar>(R.id.pbBlock).progress = percent

            container.addView(blockView)
        }
    }
}
