package com.example.groupprojectfirsttry

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter

class TimeSpentAxisValueFormatter : ValueFormatter() {
    /**
     * Предполагается, что время выполнения в нормализованном виде:
     * 0.0 - очень медленно, 1.0 - очень быстро
     */
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        // Переводим нормализованное значение в диапазон времени (например, 5–60 мин)
        val minTimeMin = 5
        val maxTimeMin = 60
        val rawMinutes = (minTimeMin + (maxTimeMin * (1.0 - value)).toInt())
        return "$rawMinutes мин"
    }
}