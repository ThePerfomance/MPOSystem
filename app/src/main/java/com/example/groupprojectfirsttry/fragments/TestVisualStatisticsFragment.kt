package com.example.groupprojectfirsttry.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
    private lateinit var btnBack: ImageButton

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
        btnBack = view.findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val testName = requireArguments().getString("testName") ?: "Неизвестный тест"
        val student = requireArguments().getParcelable<User>("student")
        val studentName = if (student != null) "${student.lastname} ${student.firstname}" else "Неизвестный студент"

        tvThemeHeader.text = testName
        tvStudentName.text = studentName

        val testStatistics =
            requireArguments().getParcelableArrayList<TestStatistic>("testStatistics")
                ?: emptyList()
        
        if (testStatistics.isNotEmpty()) {
            setupChart(view, testStatistics)
            setupRecyclerView(view, testStatistics)
        }
    }

    private fun setupChart(view: View, testStatistics: List<TestStatistic>) {
        val barChart: BarChart = view.findViewById(R.id.barChart)

        // СЧИТАЕМ ПРОЦЕНТЫ ВМЕСТО СЫРЫХ БАЛЛОВ
        val entries = testStatistics.mapIndexed { index, statistic ->
            val percentage = if (statistic.totalPoints > 0) {
                (statistic.earnedPoints.toFloat() / statistic.totalPoints.toFloat()) * 100f
            } else {
                0f
            }
            BarEntry(index.toFloat(), percentage)
        }

        val dataSet = BarDataSet(entries, getString(R.string.test_results_result_label)).apply {
            color = context?.getColor(R.color.AccentColor) ?: Color.RED
            valueTextColor = Color.DKGRAY
            valueTextSize = 10f
            setDrawValues(true)
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        barChart.apply {
            data = barData
            
            // Настройка осей
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = Color.GRAY
                textSize = 10f
                valueFormatter = IndexAxisValueFormatter(testStatistics.mapIndexed { index, _ -> "${index + 1}" })
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 20f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EEEEEE")
                textColor = Color.GRAY
                textSize = 10f
            }

            axisRight.isEnabled = false
            
            // Настройка легенды
            legend.apply {
                isEnabled = false // Скрываем, так как заголовок карточки уже говорит о чем график
            }

            description.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            animateY(1000)
            extraBottomOffset = 10f
        }

        barChart.invalidate()
    }

    private fun setupRecyclerView(view: View, testStatistics: List<TestStatistic>) {
        val recyclerViewAttempts: RecyclerView = view.findViewById(R.id.recyclerViewAttempts)
        recyclerViewAttempts.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewAttempts.isNestedScrollingEnabled = false

        val adapter = TestAttemptAdapter(testStatistics)
        recyclerViewAttempts.adapter = adapter
    }
}
