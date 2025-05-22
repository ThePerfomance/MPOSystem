package com.example.groupprojectfirsttry

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter

class AccuracyAxisValueFormatter : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val percent = (value * 100).toInt()
        return "$percent%"
    }
}