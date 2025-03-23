package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.api.TestStatistic

class TestStatisticAdapter(private val results: List<TestStatistic>) : RecyclerView.Adapter<TestStatisticAdapter.TestResultViewHolder>() {

    class TestResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvScore: TextView = itemView.findViewById(R.id.tvScore)
        val tvCompletedAt: TextView = itemView.findViewById(R.id.tvCompletedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_test_statistic, parent, false)
        return TestResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: TestResultViewHolder, position: Int) {
        val result = results[position]
        holder.tvScore.text = "Оценка: ${result.score}"
        holder.tvCompletedAt.text = "Дата завершения: ${result.completed_at}"
    }

    override fun getItemCount(): Int = results.size
}