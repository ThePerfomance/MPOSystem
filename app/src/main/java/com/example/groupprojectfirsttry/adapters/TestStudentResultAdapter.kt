package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.api.TestStatistic

class TestStudentResultAdapter(
    private val testStatistics: List<TestStatistic>,
    private val allTestStatistics: List<TestStatistic> // Полный список для расчетов
) : RecyclerView.Adapter<TestStudentResultAdapter.TestResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.test_student_result_item, parent, false)
        return TestResultViewHolder(view, allTestStatistics) // Передаем allTestStatistics
    }

    override fun onBindViewHolder(holder: TestResultViewHolder, position: Int) {
        val testStatistic = testStatistics[position]
        holder.bind(testStatistic)
    }

    override fun getItemCount(): Int = testStatistics.size

    class TestResultViewHolder(
        itemView: View,
        private val allTestStatistics: List<TestStatistic> // Добавляем allTestStatistics
    ) : RecyclerView.ViewHolder(itemView) {

        private val testName: TextView = itemView.findViewById(R.id.testName)
        private val attemptsCount: TextView = itemView.findViewById(R.id.attemptsCount)
        private val bestScore: TextView = itemView.findViewById(R.id.bestScore)

        fun bind(testStatistic: TestStatistic) {
            testName.text = "Тест ${testStatistic.test_id}"
            val attempts = allTestStatistics.count { it.test_id == testStatistic.test_id }
            attemptsCount.text = "Попыток: $attempts"
            val bestScoreValue = allTestStatistics
                .filter { it.test_id == testStatistic.test_id }
                .maxByOrNull { it.score }?.score
            bestScore.text = "Лучший результат: ${bestScoreValue ?: 0}"
        }
    }
}