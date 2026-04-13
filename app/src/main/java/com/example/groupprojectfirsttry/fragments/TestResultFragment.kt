package com.example.groupprojectfirsttry.fragments

import android.graphics.Color
import android.os.Bundle
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
    private lateinit var results: List<ResultItem>
    private var score = 0
    private var totalQuestions = 0
    private lateinit var tvUpperLeftCorner: TextView
    private lateinit var tvUpperCenter: TextView
    private lateinit var clUpHead: ConstraintLayout
    private lateinit var bnmDown: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tvUpperLeftCorner = requireActivity().findViewById(R.id.textViewLeftUpperCorner)
        tvUpperCenter = requireActivity().findViewById(R.id.textViewUpper)
        clUpHead = requireActivity().findViewById(R.id.constraintLayoutUpHead)
        bnmDown = requireActivity().findViewById(R.id.bottom_nav)

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)

        tvUpperCenter.text = requireArguments().getString("testTitle")

        val parcelableList = requireArguments().getParcelableArrayList<ResultItem>("results")
            ?: throw IllegalArgumentException("Results not found")
        results = parcelableList
        score = requireArguments().getInt("score")
        totalQuestions = requireArguments().getInt("totalQuestions")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val resultsList = view.findViewById<RecyclerView>(R.id.resultsList)
        resultsList.layoutManager = LinearLayoutManager(context)

        resultsAdapter = ResultAdapter(results)
        val itemDecorator = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divider_item!!)
            ?.let { itemDecorator.setDrawable(it) }
        resultsList.addItemDecoration(itemDecorator)
        resultsList.adapter = resultsAdapter

        val tvScore = view.findViewById<TextView>(R.id.textViewScore)
        val tvScorePercentage = view.findViewById<TextView>(R.id.textViewScorePercentage)
        
        val correctPercentage = (score.toFloat() / totalQuestions.toFloat()) * 100
        
        // Логика цвета текста на основе процентов
        val resultColor = when {
            correctPercentage < 50 -> Color.parseColor("#FF0000") // Красный
            correctPercentage <= 70 -> Color.parseColor("#FFD700") // Желтый (Gold)
            else -> Color.parseColor("#4CAF50") // Зеленый
        }

        var totalMark = 0
        if (correctPercentage > 84) totalMark = 5
        else if (correctPercentage > 69) totalMark = 4
        else if (correctPercentage > 51) totalMark = 3
        else totalMark = 2
        
        tvScore.text = "Оценка $totalMark"
        tvScore.setTextColor(resultColor)
        
        tvScorePercentage.text = "${correctPercentage.toInt()}%"
        tvScorePercentage.setTextColor(resultColor)

        setupPieChart(view, correctPercentage)
    }

    private fun setupPieChart(view: View, correctPercentage: Float) {
        val pieChart = view.findViewById<PieChart>(R.id.pieChart)
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

    override fun onResume() {
        super.onResume()
        clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
    }
}