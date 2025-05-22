package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.AccuracyAxisValueFormatter
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.KMeans
import com.example.groupprojectfirsttry.KMeans.Point
import com.example.groupprojectfirsttry.TimeSpentAxisValueFormatter
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.utils.ColorTemplate

class ClusterChartFragment : Fragment() {

    private lateinit var scatterChart: ScatterChart
    private lateinit var points: List<KMeans.Point>

    companion object {
        fun newInstance(points: List<KMeans.Point>): ClusterChartFragment {
            val fragment = ClusterChartFragment()
            fragment.points = points
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cluster_chart, container, false)
        scatterChart = view.findViewById(R.id.scatterChartClusters)
        setupClusterChart(points)
        return view
    }

    private fun setupClusterChart(points: List<KMeans.Point>) {
        val clusters = points.groupBy { it.clusterId }

        // Расширяем палитру цветов
        val allColors = ColorTemplate.MATERIAL_COLORS.toList() +
                ColorTemplate.VORDIPLOM_COLORS.toList()

        val rankLabels = mapOf(
            0 to "S",
            1 to "A",
            2 to "B",
            3 to "C",
            4 to "D"
        )

        val dataSets = clusters.map { (clusterId, clusterPoints) ->
            val entries = clusterPoints.map { point ->
                Entry(point.features[0].toFloat(), // accuracy
                    point.features[2].toFloat()  // timeSpent
                )
            }

            val colorIndex = clusterId % allColors.size
            val color = allColors[colorIndex]

            val label = rankLabels[clusterId] ?: "Кластер $clusterId"

            val dataSet = ScatterDataSet(entries, label)
            dataSet.color = color
            dataSet.setScatterShape(com.github.mikephil.charting.charts.ScatterChart.ScatterShape.CIRCLE)
            dataSet.scatterShapeSize = 18f
            dataSet.setDrawValues(false)
            dataSet
        }

        val data = ScatterData(dataSets)

        with(scatterChart) {
            this.data = data
            description.text = "Ранги студентов по кластерам"
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 1f
            xAxis.valueFormatter = AccuracyAxisValueFormatter()
            xAxis.granularity = 0.2f
            xAxis.labelCount = 5
            xAxis.labelRotationAngle = -45f
            xAxis.axisLineColor = resources.getColor(android.R.color.darker_gray)
            xAxis.textColor = resources.getColor(android.R.color.black)

            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 1f
            axisLeft.valueFormatter = TimeSpentAxisValueFormatter()
            axisLeft.labelCount = 5
            axisLeft.textColor = resources.getColor(android.R.color.black)

            axisRight.isEnabled = false
            legend.isEnabled = true
            animateY(500)
            invalidate()
        }
    }
}