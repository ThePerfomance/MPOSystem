package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.adapters.TrainingSessionsAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.TrainingSession
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.launch

class TrainingListFragment : Fragment(R.layout.fragment_training_list) {

    private lateinit var rvAdaptiveSessions: RecyclerView
    private lateinit var rvTrainingSessions: RecyclerView
    private lateinit var tvAdaptiveHeader: TextView
    private lateinit var tvWorkOnErrorsHeader: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var btnBack: View
    private lateinit var shimmerTraining: ShimmerFrameLayout
    private lateinit var swipeRefreshTraining: SwipeRefreshLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvAdaptiveSessions = view.findViewById(R.id.rvAdaptiveSessions)
        rvTrainingSessions = view.findViewById(R.id.rvTrainingSessions)
        tvAdaptiveHeader = view.findViewById(R.id.tvAdaptiveHeader)
        tvWorkOnErrorsHeader = view.findViewById(R.id.tvWorkOnErrorsHeader)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        btnBack = view.findViewById(R.id.btnBack)
        shimmerTraining = view.findViewById(R.id.shimmer_training)
        swipeRefreshTraining = view.findViewById(R.id.swipeRefreshTraining)

        rvAdaptiveSessions.layoutManager = LinearLayoutManager(context)
        rvTrainingSessions.layoutManager = LinearLayoutManager(context)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupSwipeRefresh()
        loadTrainingSessions(isRefresh = false)
    }

    private fun setupSwipeRefresh() {
        swipeRefreshTraining.setColorSchemeResources(R.color.AccentColor)
        swipeRefreshTraining.setOnRefreshListener {
            loadTrainingSessions(isRefresh = true)
        }
    }

    private fun startLoading() {
        shimmerTraining.visibility = View.VISIBLE
        shimmerTraining.startShimmer()
        rvAdaptiveSessions.visibility = View.GONE
        rvTrainingSessions.visibility = View.GONE
        tvAdaptiveHeader.visibility = View.GONE
        tvWorkOnErrorsHeader.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
    }

    private fun stopLoading() {
        shimmerTraining.stopShimmer()
        shimmerTraining.visibility = View.GONE
    }

    private fun loadTrainingSessions(isRefresh: Boolean = false) {
        val userProvider = requireActivity() as? UserProvider ?: return
        val user = userProvider.getUser()
        val userId = user.id ?: return

        if (!isRefresh) {
            startLoading()
        }

        lifecycleScope.launch {
            try {
                val sessions = ApiClient.apiService.getTrainingSessions(userId)
                
                // 1. Адаптивные сессии (без привязки к конкретному результату теста)
                val adaptiveSessions = sessions.filter { 
                    it.sourceTestResultId == null && it.questions?.any { q -> !q.status.equals("correct", true) } == true
                }.sortedByDescending { it.createdAt }

                // 2. Обычные сессии (Работа над ошибками)
                val errorSessions = sessions.filter { 
                    it.sourceTestResultId != null && it.questions?.any { q -> !q.status.equals("correct", true) } == true
                }.sortedByDescending { it.createdAt }

                if (adaptiveSessions.isEmpty() && errorSessions.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    tvAdaptiveHeader.visibility = View.GONE
                    tvWorkOnErrorsHeader.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    
                    // Показываем адаптивный блок
                    if (adaptiveSessions.isNotEmpty()) {
                        tvAdaptiveHeader.visibility = View.VISIBLE
                        rvAdaptiveSessions.visibility = View.VISIBLE
                        rvAdaptiveSessions.adapter = TrainingSessionsAdapter(adaptiveSessions) { session ->
                            navigateToTraining(session, true)
                        }
                    } else {
                        tvAdaptiveHeader.visibility = View.GONE
                        rvAdaptiveSessions.visibility = View.GONE
                    }

                    // Показываем блок ошибок
                    if (errorSessions.isNotEmpty()) {
                        tvWorkOnErrorsHeader.visibility = View.VISIBLE
                        rvTrainingSessions.visibility = View.VISIBLE
                        rvTrainingSessions.adapter = TrainingSessionsAdapter(errorSessions) { session ->
                            navigateToTraining(session, false)
                        }
                    } else {
                        tvWorkOnErrorsHeader.visibility = View.GONE
                        rvTrainingSessions.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("TrainingListFragment", "Error loading sessions", e)
                tvEmptyState.text = "Ошибка загрузки списка"
                tvEmptyState.visibility = View.VISIBLE
            } finally {
                if (isAdded) {
                    if (!isRefresh) stopLoading()
                    swipeRefreshTraining.isRefreshing = false
                }
            }
        }
    }

    private fun navigateToTraining(session: TrainingSession, isAdaptive: Boolean) {
        val bundle = Bundle().apply {
            putParcelable("session", session)
            putBoolean("is_adaptive", isAdaptive)
        }
        (requireActivity() as? SecondActivityWithBottomNavMenu)?.replaceFragment(TrainingFragment(), bundle)
    }
}
