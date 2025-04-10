package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.Test
import com.example.groupprojectfirsttry.api.TestStatistic

class TestsAdapter(
    private var tests: List<Test>,
    private val onArrowClick: (Test) -> Unit, // Клик по стрелке (запуск теста) // Клик по "Статистика"
) : RecyclerView.Adapter<TestsAdapter.TestViewHolder>() {

    private var testResults: Map<Int, List<TestStatistic>> = emptyMap()

    class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChapterName: TextView = itemView.findViewById(R.id.tvChapterName)
        val ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.test_item, parent, false)
        return TestViewHolder(view)
    }

    // Метод для обновления данных
    fun updateTests(newTests: List<Test>, newTestResults: Map<Int, List<TestStatistic>>) {
        tests = newTests
        testResults = newTestResults
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        val test = tests[position]

        holder.tvChapterName.text = "${position + 1}. ${test.title}"

        // Обработчики кликов
        holder.ivArrow.setOnClickListener {
            if (position == 0 || (position > 0 && getMaxScoreForTest(tests[position - 1]) >= 5)) {
                onArrowClick(test)
            } else {
                Toast.makeText(holder.itemView.context, "Пройдите предыдущий тест минимум на 5 баллов", Toast.LENGTH_SHORT).show()
            }
        }

        // Устанавливаем доступность стрелки
        if (position > 0 && getMaxScoreForTest(tests[position - 1]) < 5) {
            holder.ivArrow.setImageResource(R.drawable.ic_lock) // Замените на вашу иконку блокировки
            holder.ivArrow.isEnabled = false
        } else {
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_right) // Замените на вашу иконку стрелки
            holder.ivArrow.isEnabled = true
        }
    }

    private fun getMaxScoreForTest(test: Test): Int {
        return testResults[test.id]?.maxOfOrNull { it.score } ?: 0
    }

    override fun getItemCount() = tests.size
}