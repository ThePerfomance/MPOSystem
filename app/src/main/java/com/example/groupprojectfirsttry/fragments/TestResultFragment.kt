package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.ResultItem
import com.example.groupprojectfirsttry.adapters.ResultAdapter
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.api.ApiClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class TestResultFragment : Fragment(R.layout.fragment_test_result) {

    private lateinit var resultsAdapter: ResultAdapter
    private var results: List<ResultItem> = emptyList()
    private var score = 0
    private var totalQuestions = 0
    private var resultId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Activity UI setup (Top bar title)
        val tvUpperCenter = requireActivity().findViewById<TextView>(R.id.textViewUpper)
        tvUpperCenter?.text = arguments?.getString("testTitle") ?: "Результаты"

        // Data setup
        val parcelableList = arguments?.getParcelableArrayList<ResultItem>("results")
        results = parcelableList ?: emptyList()
        score = arguments?.getInt("score") ?: 0
        totalQuestions = arguments?.getInt("totalQuestions") ?: results.size
        resultId = arguments?.getString("resultId")

        // 1. RecyclerView setup (Question list with selected answers)
        val resultsList = view.findViewById<RecyclerView>(R.id.resultsList)
        resultsList.layoutManager = LinearLayoutManager(context)
        resultsAdapter = ResultAdapter(results)
        resultsList.adapter = resultsAdapter

        // 2. Summary Card (Plashka at the end)
        val tvCorrectAnswersCount = view.findViewById<TextView>(R.id.tvCorrectAnswersCount)
        val tvResultStatus = view.findViewById<TextView>(R.id.tvResultStatus)
        val llFinalResultCard = view.findViewById<LinearLayout>(R.id.llFinalResultCard)
        
        tvCorrectAnswersCount.text = "Правильных ответов: $score из $totalQuestions"
        
        val percentage = if (totalQuestions > 0) (score * 100) / totalQuestions else 0
        
        if (percentage >= 80) {
            tvResultStatus.text = "Отличный результат!"
            tvResultStatus.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
            llFinalResultCard.setBackgroundResource(R.drawable.bg_result_summary_green)
        } else {
            tvResultStatus.text = "Попробуйте ещё раз"
            tvResultStatus.setTextColor(android.graphics.Color.parseColor("#7B1D1D"))
            llFinalResultCard.setBackgroundResource(R.drawable.bg_result_summary)
        }

        // 3. Action Buttons
        val btnRetryTest = view.findViewById<MaterialButton>(R.id.btnRetryTest)
        btnRetryTest.setOnClickListener {
            // Pop back to the test screen or previous screen
            parentFragmentManager.popBackStack()
        }

        val btnGoToTraining = view.findViewById<MaterialButton>(R.id.btnGoToTraining)
        if (percentage < 100 && resultId != null) {
            btnGoToTraining.visibility = View.VISIBLE
            btnGoToTraining.setOnClickListener {
                (requireActivity() as? SecondActivityWithBottomNavMenu)
                    ?.replaceFragment(TrainingListFragment(), null)
            }
            // Auto-create training session so it's ready for the user
            autoCreateTrainingSession(resultId!!)
        } else {
            btnGoToTraining.visibility = View.GONE
        }
    }

    private fun autoCreateTrainingSession(id: String) {
        lifecycleScope.launch {
            try {
                ApiClient.apiService.createTrainingSession(id)
                Log.d("TestResultFragment", "Training auto-created for result: $id")
            } catch (e: Exception) {
                Log.e("TestResultFragment", "Auto-creation failed", e)
            }
        }
    }
}
