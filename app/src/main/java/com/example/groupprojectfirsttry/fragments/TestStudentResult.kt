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
import com.example.groupprojectfirsttry.api.TestStatistic
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

        val userId = requireArguments().getSerializable("userId") as UUID
        if (userId != null) {
            loadTestResults(userId)
        }
    }

    private fun loadTestResults(userId: UUID) = lifecycleScope.launch {
        try {
            // Получаем статистику тестов пользователя
            val testStatistics = ApiClient.apiService.getUserTestResults(userId)
            Log.d("TestResultsFragment", "Received ${testStatistics.size} test results for user ID: $userId")

            // Получаем количество вопросов для каждого теста
            val testQuestionCounts = mutableMapOf<Int, Int>()
            testStatistics.forEach { statistic ->
                val testId = statistic.test_id
                if (!testQuestionCounts.containsKey(testId)) {
                    val questions = ApiClient.apiService.getQuestions(testId)
                    testQuestionCounts[testId] = questions.size
                }
            }

            // Загружаем имена тестов
            val tests = ApiClient.apiService.getTests()
            val testNames = tests.associate { it.id to it.title } // Создаем карту test_id -> name

            // Передаем данные в адаптер
            adapter = TestStudentResultAdapter(
                testStatistics,
                testStatistics,
                testQuestionCounts,
                testNames, // Передаем имена тестов
                object : TestStudentResultAdapter.OnStatisticsClickListener {
                    override fun onStatisticsClicked(testStatistic: TestStatistic) {
                        Log.d("TestResultsFragment", "Statistics clicked for test ID: ${testStatistic.test_id}")
                        // Здесь можно открыть другой фрагмент или выполнить другие действия
                    }
                }
            )
            recyclerView.adapter = adapter
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TestResultsFragment", "Error fetching test results: ${e.message}")
        }
    }
}