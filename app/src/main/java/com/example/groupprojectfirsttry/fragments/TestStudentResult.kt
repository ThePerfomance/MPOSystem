package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.adapters.TestStudentResultAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import kotlinx.coroutines.launch
import java.util.UUID

class TestStudentResult : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TestStudentResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_test_student_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewTestResults)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val userId=requireArguments().getSerializable("userId") as UUID
        if (userId != null) {
            loadTestResults(userId)
        }
    }

    private fun loadTestResults(userId: UUID) = lifecycleScope.launch {
        try {
            val testStatistics = ApiClient.apiService.getUserTestResults(userId)
            Log.d("TestResultsFragment", "Received ${testStatistics.size} test results for user ID: $userId")
            adapter = TestStudentResultAdapter(testStatistics, testStatistics) // Передаем полный список
            recyclerView.adapter = adapter
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TestResultsFragment", "Error fetching test results: ${e.message}")
        }
    }
}