package com.example.groupprojectfirsttry.fragments

import android.graphics.Color
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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.adapters.TestAttemptAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TestStatistic
import com.example.groupprojectfirsttry.simpleClasses.User
import kotlinx.coroutines.launch

class TestVisualStatisticsFragment : Fragment() {

    private lateinit var tvThemeHeader: TextView
    private lateinit var tvStudentName: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_visual_test_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvThemeHeader = view.findViewById(R.id.testThemeHeader)
        tvStudentName = view.findViewById(R.id.studentName)

        val testName = requireArguments().getString("testName") ?: "Неизвестный тест"
        val student = requireArguments().getParcelable<User>("student")
        val studentName = "${student?.lastname} ${student?.firstname}" ?: "Неизвестный студент"

        tvThemeHeader.text = testName
        tvStudentName.text = studentName

        val testStatistics =
            requireArguments().getParcelableArrayList<TestStatistic>("testStatistics")
                ?: emptyList()
        
        if (testStatistics.isNotEmpty()) {
            val testId = testStatistics[0].test_id
            lifecycleScope.launch {
                try {
                    // ВАЖНО: Мы не запрашиваем количество вопросов здесь, 
                    // так как в TestStatistic.score УЖЕ хранится процент (0-100)
                    // Это подтверждается кодом в TestPassFragment.kt
                    
                    val barChart: BarChart = view.findViewById(R.id.barChart)

                    val entries = mutableListOf<BarEntry>()
                    for ((index, statistic) in testStatistics.withIndex()) {
                        // score уже является процентом
                        val percentageScore = statistic.score.toFloat()
                        entries.add(BarEntry(index.toFloat(), percentageScore))
                    }

                    val dataSet = BarDataSet(entries, "Результаты теста").apply {
                        color = resources.getColor(R.color.StatisticChartMainColor)
                        valueTextColor = Color.BLACK
                        valueTextSize = 12f
                    }

                    val barData = BarData(dataSet).apply {
                        barWidth = 0.5f
                        setValueTextSize(12f)
                    }

                    barChart.apply {
                        data = barData
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            granularity = 1f
                            setDrawGridLines(false)
                            textColor = Color.BLACK
                            textSize = 12f
                            valueFormatter = IndexAxisValueFormatter(testStatistics.mapIndexed { index, _ -> "${index + 1}" })
                        }

                        axisLeft.apply {
                            axisMinimum = 0f
                            axisMaximum = 100f
                            granularity = 10f
                            setDrawGridLines(true)
                            textColor = Color.BLACK
                            textSize = 12f
                        }

                        axisRight.isEnabled = false
                        legend.apply {
                            isEnabled = true
                            textSize = 14f
                            textColor = Color.BLACK
                            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                            orientation = Legend.LegendOrientation.HORIZONTAL
                        }

                        description.isEnabled = false
                        setTouchEnabled(true)
                        animateY(1000)
                    }

                    barChart.invalidate()
                    
                    val recyclerViewAttempts: RecyclerView = view.findViewById(R.id.recyclerViewAttempts)
                    recyclerViewAttempts.layoutManager = LinearLayoutManager(requireContext())

                    // Передаем 100 в качестве "количества вопросов", так как score - это процент
                    val adapter = TestAttemptAdapter(testStatistics, 100)
                    recyclerViewAttempts.adapter = adapter

                } catch (e: Exception) {
                    Log.e("TestVisualStatisticsFragment", "Ошибка: ${e.message}")
                }
            }
        }
    }
}
