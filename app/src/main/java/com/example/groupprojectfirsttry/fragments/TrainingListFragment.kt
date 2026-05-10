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

    private lateinit var rvTrainingSessions: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var btnBack: View
    private lateinit var shimmerTraining: ShimmerFrameLayout
    private lateinit var swipeRefreshTraining: SwipeRefreshLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTrainingSessions = view.findViewById(R.id.rvTrainingSessions)
        pbLoading = view.findViewById(R.id.pbLoading)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        btnBack = view.findViewById(R.id.btnBack)
        shimmerTraining = view.findViewById(R.id.shimmer_training)
        swipeRefreshTraining = view.findViewById(R.id.swipeRefreshTraining)

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
        rvTrainingSessions.visibility = View.GONE
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
                // Сервер возвращает сессии только для данного userId
                val sessions = ApiClient.apiService.getTrainingSessions(userId)
                
                // Фильтруем сессии, в которых есть нерешенные вопросы (status != "correct")
                val sessionsWithErrors = sessions.filter { session ->
                    session.questions?.any { it.status != "correct" } == true
                }.sortedByDescending { it.createdAt }

                if (sessionsWithErrors.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    rvTrainingSessions.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    rvTrainingSessions.visibility = View.VISIBLE
                    rvTrainingSessions.adapter = TrainingSessionsAdapter(sessionsWithErrors) { session ->
                        navigateToTraining(session)
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

    private fun navigateToTraining(session: TrainingSession) {
        val bundle = Bundle().apply {
            putParcelable("session", session)
        }
        (requireActivity() as? SecondActivityWithBottomNavMenu)?.replaceFragment(TrainingFragment(), bundle)
    }
}
