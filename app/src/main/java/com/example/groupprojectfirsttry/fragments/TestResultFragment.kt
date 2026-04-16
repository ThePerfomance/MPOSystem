package com.example.groupprojectfirsttry.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.ResultItem
import com.example.groupprojectfirsttry.adapters.ResultAdapter
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView

class TestResultFragment : Fragment(R.layout.fragment_test_result) {

    private lateinit var resultsAdapter: ResultAdapter
    private var results: List<ResultItem> = emptyList()
    private var score = 0
    private var totalQuestions = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Activity UI setup
        val tvUpperCenter = requireActivity().findViewById<TextView>(R.id.textViewUpper)
        val clUpHead = requireActivity().findViewById<ConstraintLayout>(R.id.constraintLayoutUpHead)
        val bnmDown = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)

        tvUpperCenter?.text = arguments?.getString("testTitle") ?: "Результаты"
        clUpHead?.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
        bnmDown?.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)

        // Data setup
        val parcelableList = arguments?.getParcelableArrayList<ResultItem>("results")
        if (parcelableList == null) {
            Log.e("TestResultFragment", "Results not found in arguments")
            return
        }
        results = parcelableList
        score = arguments?.getInt("score") ?: 0
        totalQuestions = arguments?.getInt("totalQuestions") ?: results.size

        // RecyclerView setup
        val resultsList = view.findViewById<RecyclerView>(R.id.resultsList)
        resultsList.layoutManager = LinearLayoutManager(context)
        resultsAdapter = ResultAdapter(results)
        
        val itemDecorator = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divider_item)?.let {
            itemDecorator.setDrawable(it)
        }
        resultsList.addItemDecoration(itemDecorator)
        resultsList.adapter = resultsAdapter

        // Score display
        val tvScore = view.findViewById<TextView>(R.id.textViewScore)
        val tvScorePercentage = view.findViewById<TextView>(R.id.textViewScorePercentage)
        
        val correctPercentage = if (totalQuestions > 0) (score.toFloat() / totalQuestions.toFloat()) * 100 else 0f
        
        val resultColor = when {
            correctPercentage < 50 -> Color.parseColor("#FF0000")
            correctPercentage <= 70 -> Color.parseColor("#FFD700")
            else -> Color.parseColor("#4CAF50")
        }

        val totalMark = when {
            correctPercentage > 84 -> 5
            correctPercentage > 69 -> 4
            correctPercentage > 51 -> 3
            else -> 2
        }
        
        tvScore.text = "Оценка $totalMark"
        tvScore.setTextColor(resultColor)
        
        tvScorePercentage.text = "${correctPercentage.toInt()}%"
        tvScorePercentage.setTextColor(resultColor)

        setupPieChart(view, correctPercentage)
    }

    private fun setupPieChart(view: View, correctPercentage: Float) {
        val pieChart = view.findViewById<PieChart>(R.id.pieChart) ?: return
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = false
            setEntryLabelTextSize(14f)
            setEntryLabelColor(Color.WHITE)
            setDrawEntryLabels(true)
            centerText = ""
            holeRadius = 50f
            transparentCircleRadius = 0f
            animateXY(1000, 1000)
            rotationAngle = 90f
        }

        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(correctPercentage))
        entries.add(PieEntry(100f - correctPercentage))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.GraphicCorrectColor),
            ContextCompat.getColor(requireContext(), R.color.GraphicInCorrectColor)
        )
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 3f

        pieChart.data = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = "${value.toInt()}%"
            })
        }
        pieChart.invalidate()
    }
}
