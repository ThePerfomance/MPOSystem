package com.example.groupprojectfirsttry.fragments

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

        // Вернул стандартный градиент для верхней и нижней панели
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
        val tvScorePercentage= view.findViewById<TextView>(R.id.textViewScorePercentage)
        val correctPercentage = (score.toFloat() / totalQuestions.toFloat()) * 100
        var totalMark=0
        if (correctPercentage>84) totalMark=5
        else if(correctPercentage>69) totalMark=4
        else if(correctPercentage>51) totalMark=3
        else totalMark=2
        
        tvScore.text = "Оценка $totalMark"
        tvScorePercentage.text = "${correctPercentage.toInt()}%"

        val pieChart = view.findViewById<PieChart>(R.id.pieChart)
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = false
            setEntryLabelTextSize(14f)
            setEntryLabelColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            setDrawEntryLabels(true)
            centerText = ""
            setCenterTextSize(24f)
            holeRadius = 50f
            transparentCircleRadius = 0f
            animateXY(1000,1000)
            rotationAngle = 90f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
        }

        val entries = ArrayList<PieEntry>()
        val incorrectPercentage = 100 - correctPercentage
        entries.add(PieEntry(correctPercentage))
        entries.add(PieEntry(incorrectPercentage))

        val dataSet = PieDataSet(entries, "")
        dataSet.valueTextSize=14f
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(),R.color.GraphicCorrectColor),
            ContextCompat.getColor(requireContext(), R.color.GraphicInCorrectColor)
        )

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "${String.format("%.0f", value)}%"
        })
        pieChart.data = pieData
        pieChart.highlightValues(null)
        pieChart.invalidate()
    }

    override fun onPause() {
        super.onPause()
        tvUpperCenter.text = ""
        tvUpperCenter.visibility = View.VISIBLE
        tvUpperLeftCorner.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        tvUpperCenter.text = requireArguments().getString("testTitle")
        tvUpperCenter.visibility = View.VISIBLE
        tvUpperLeftCorner.visibility = View.GONE

        // Убеждаемся, что при возвращении цвета остаются правильными
        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
    }

    override fun onDestroy() {
        super.onDestroy()
        tvUpperCenter.text = ""
        tvUpperCenter.visibility = View.VISIBLE
        tvUpperLeftCorner.visibility = View.GONE
    }
}