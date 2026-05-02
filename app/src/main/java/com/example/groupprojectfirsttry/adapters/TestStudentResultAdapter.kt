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
    private val allTestStatistics: List<TestStatistic>,
    private val testNames: Map<Int, String>, // testQuestionCounts больше не нужен!
    private val onStatisticsClickListener: OnStatisticsClickListener
) : RecyclerView.Adapter<TestStudentResultAdapter.TestResultViewHolder>() {

    private val uniqueTestStatistics = testStatistics.distinctBy { it.test_id }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.test_student_result_item, parent, false)
        return TestResultViewHolder(view, allTestStatistics, testNames, onStatisticsClickListener)
    }

    override fun onBindViewHolder(holder: TestResultViewHolder, position: Int) {
        holder.bind(uniqueTestStatistics[position])
    }

    override fun getItemCount(): Int = uniqueTestStatistics.size

    class TestResultViewHolder(
        itemView: View,
        private val allTestStatistics: List<TestStatistic>,
        private val testNames: Map<Int, String>,
        private val onStatisticsClickListener: OnStatisticsClickListener
    ) : RecyclerView.ViewHolder(itemView) {

        private val testName: TextView = itemView.findViewById(R.id.testName)
        private val attemptsCount: TextView = itemView.findViewById(R.id.attemptsCount)
        private val bestScore: TextView = itemView.findViewById(R.id.bestScore)
        private val statisticsLink: View = itemView.findViewById(R.id.statisticsLink)
        private val bestMark: TextView = itemView.findViewById(R.id.textViewbestMark)

        fun bind(testStatistic: TestStatistic) {
            val testNameString = testNames[testStatistic.test_id] ?: "Неизвестный тест"
            testName.text = "Тема ${testStatistic.test_id}. $testNameString"

            // Ищем все попытки этого теста
            val testAttempts = allTestStatistics.filter { it.test_id == testStatistic.test_id }
            attemptsCount.text = testAttempts.size.toString()

            // Находим лучшую попытку (по количеству набранных баллов)
            val bestAttempt = testAttempts.maxByOrNull { it.score }

            // Считаем правильный процент: (Набранные баллы / Максимально возможные баллы) * 100
            val percentageScore = if (bestAttempt != null && bestAttempt.totalPoints > 0) {
                (bestAttempt.score.toDouble() / bestAttempt.totalPoints * 100).toInt()
            } else {
                0
            }

            val totalMark = calculateGrade(percentageScore)

            bestScore.text = "$percentageScore%"
            bestMark.text = totalMark.toString()

            // Можно нажимать на всю карточку (statisticsLink можно перевесить на саму itemView)
            itemView.setOnClickListener {
                onStatisticsClickListener.onStatisticsClicked(testStatistic)
            }
            // Оставляем и для кнопки, если она есть
            statisticsLink.setOnClickListener {
                onStatisticsClickListener.onStatisticsClicked(testStatistic)
            }
        }
    }

    interface OnStatisticsClickListener {
        fun onStatisticsClicked(testStatistic: TestStatistic)
    }
}

private fun calculateGrade(percentageScore: Int): Int {
    return when {
        percentageScore > 84 -> 5
        percentageScore > 69 -> 4
        percentageScore > 51 -> 3
        else -> 2
    }
}