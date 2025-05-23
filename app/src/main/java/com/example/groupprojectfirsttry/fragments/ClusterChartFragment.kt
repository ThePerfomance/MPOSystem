package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.MathMethods.KMeans.Point
import com.example.groupprojectfirsttry.MathMethods.KMeans
import com.example.groupprojectfirsttry.MathMethods.KMeans.getCentroid
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.utils.ColorTemplate

class ClusterChartFragment : Fragment() {

    private lateinit var scatterChart: ScatterChart
    private lateinit var points: List<Point>

    companion object {
        fun newInstance(points: List<Point>): ClusterChartFragment {
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

    private fun setupClusterChart(points: List<Point>) {
        val clusters = points.groupBy { it.clusterId }
        val allColors = ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.VORDIPLOM_COLORS.toList()
        val rankLabels = mapOf(
            0 to "S",
            1 to "A",
            2 to "B",
            3 to "C",
            4 to "D"
        )

        val dataSets = clusters.map { (clusterId, clusterPoints) ->
            val entries = clusterPoints.map { point ->
                Entry(point.features[0].toFloat(), point.features[1].toFloat())
            }

            val dataSet = ScatterDataSet(entries, rankLabels[clusterId] ?: "Кластер $clusterId")
            dataSet.color = allColors[clusterId % allColors.size]
            dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE)
            dataSet.scatterShapeSize = 30f
            dataSet.setDrawValues(false)
            dataSet
        }

        val centroidDataSets = clusters.map { (clusterId, clusterPoints) ->
            val centroid = clusterPoints.getCentroid()
            val entry = Entry(centroid.features[0].toFloat(), centroid.features[1].toFloat())
            val dataSet = ScatterDataSet(listOf(entry), "ц.$clusterId")
            dataSet.color = allColors[clusterId % allColors.size]
            dataSet.setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
            dataSet.scatterShapeSize = 40f
            dataSet.setDrawValues(false)
            dataSet
        }

        val scatterData = ScatterData((dataSets + centroidDataSets))
        scatterChart.data = scatterData

        with(scatterChart) {
            description.text = "Кластеры после PCA"
            description.textSize=10f
            xAxis.axisMinimum = -1f
            xAxis.axisMaximum = 1f
            axisLeft.axisMinimum = -1f
            axisLeft.axisMaximum = 1f
            legend.isEnabled = true
            animateY(1000)
            invalidate()
        }
    }
}