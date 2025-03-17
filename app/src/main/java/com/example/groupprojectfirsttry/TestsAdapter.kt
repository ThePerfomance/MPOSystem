package com.example.groupprojectfirsttry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TestsAdapter(
    private var tests: List<Test>,
    private val onArrowClick: (Test) -> Unit, // Клик по стрелке (запуск теста)
    private val onStatisticsClick: (Test) -> Unit // Клик по "Статистика"
) : RecyclerView.Adapter<TestsAdapter.TestViewHolder>() {

    class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChapterName: TextView = itemView.findViewById(R.id.tvChapterName)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        val tvStatistics: TextView = itemView.findViewById(R.id.tvStatistics)
        val ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.test_item, parent, false)
        return TestViewHolder(view)
    }
    // Метод для обновления данных
    fun updateTests(newTests: List<Test>) {
        tests = newTests
        notifyDataSetChanged()
    }
    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        val test = tests[position]

        holder.tvChapterName.text = test.title
        holder.tvProgress.text = "Прогресс: ${test.progress}%"

        // Обработчики кликов
        holder.ivArrow.setOnClickListener {
            onArrowClick(test)
        }
        holder.tvStatistics.setOnClickListener {
            onStatisticsClick(test)
        }
    }

    override fun getItemCount() = tests.size
}