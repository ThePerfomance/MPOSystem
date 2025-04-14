package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.adapters.TestStudentResultAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TestStatistic
import com.example.groupprojectfirsttry.simpleClasses.User
import kotlinx.coroutines.launch
import java.util.UUID

class TestStudentResult : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TestStudentResultAdapter
    private lateinit var user:User
    private lateinit var tvHeader:TextView

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

        // Получаем данные из аргументов
        user= requireArguments().getParcelable("user")!!
        tvHeader=view.findViewById(R.id.textViewHeader)
        if (user.id != null) {
            loadTestResults(user.id!!)
            tvHeader.text=tvHeader.text.toString()+"\n"+user.lastname+" "+user.firstname
        }
    }

    private fun loadTestResults(userId: UUID) = lifecycleScope.launch {
        try {
            // Получаем статистику тестов пользователя
            val testStatistics = ApiClient.apiService.getUserTestResults(userId)
            Log.d("TestResultsFragment", "Received ${testStatistics.size} test results for user ID: $userId")

            // Сортируем статистику по test_id
            val sortedTestStatistics = testStatistics.sortedBy { it.test_id }

            // Получаем количество вопросов для каждого теста
            val testQuestionCounts = mutableMapOf<Int, Int>()
            sortedTestStatistics.forEach { statistic ->
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
                sortedTestStatistics,
                sortedTestStatistics,
                testQuestionCounts,
                testNames,
                object : TestStudentResultAdapter.OnStatisticsClickListener {
                    override fun onStatisticsClicked(testStatistic: TestStatistic) {
                        Log.d("TestResultsFragment", "Statistics clicked for test ID: ${testStatistic.test_id}")

                        // Фильтруем все результаты теста для данного test_id
                        val filteredStatistics = sortedTestStatistics.filter { it.test_id == testStatistic.test_id }

                        // Создаем Bundle для передачи данных
                        val bundle = Bundle().apply {
                            putParcelableArrayList("testStatistics", ArrayList(filteredStatistics))
                            putString("testName", "Тема ${testStatistic.test_id}. ${testNames[testStatistic.test_id]}")
                            putParcelable("student", user)
                        }

                        // Создаем фрагмент и передаем ему данные
                        val fragment = TestVisualStatisticsFragment().apply {
                            arguments = bundle
                        }

                        // Открываем фрагмент
                        (requireActivity() as SecondActivityWithBottomNavMenu).replaceFragment(fragment, bundle)
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