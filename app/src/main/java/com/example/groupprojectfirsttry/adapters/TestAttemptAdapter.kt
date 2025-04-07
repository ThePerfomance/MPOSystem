package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.api.TestStatistic
import java.text.SimpleDateFormat

class TestAttemptAdapter(private val attempts: List<TestStatistic>, private val questionCount: Int) :
    RecyclerView.Adapter<TestAttemptAdapter.AttemptViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttemptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_test_attempt, parent, false)
        return AttemptViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttemptViewHolder, position: Int) {
        val attempt = attempts[position]
        holder.bind(attempt, questionCount)
    }

    override fun getItemCount(): Int = attempts.size

    inner class AttemptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Получаем ссылки на элементы разметки
        private val tvAttemptNumber: TextView = itemView.findViewById(R.id.tvAttemptNumber)
        private val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)
        private val tvEndTime: TextView = itemView.findViewById(R.id.tvEndTime)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)
        private val tvGrade: TextView = itemView.findViewById(R.id.tvGrade)
        private val llVisualStudentStatistic: LinearLayout = itemView.findViewById(R.id.llVisualStudentStatistic)

        fun bind(attempt: TestStatistic, questionCount: Int) {
            tvAttemptNumber.text = (adapterPosition + 1).toString()
            tvStartTime.text = formatTimestamp(attempt.started_at)
            tvEndTime.text = formatTimestamp(attempt.completed_at)
            tvDuration.text = calculateDuration(attempt.started_at, attempt.completed_at)
            val percentageScore = calculatePercentageScore(attempt.score, questionCount)
            tvScore.text = "$percentageScore%"
            tvGrade.text = calculateGrade(percentageScore).toString()
            if ((adapterPosition + 1) % 2 == 0) {
                llVisualStudentStatistic.setBackgroundColor(itemView.context.getColor(R.color.LightBlueForList))
            } else {
                llVisualStudentStatistic.setBackgroundColor(itemView.context.getColor(android.R.color.transparent))
            }
        }
        fun formatTimestamp(timestamp: String?): String {
            if (timestamp.isNullOrEmpty()) {
                return "---"
            }
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") // Формат входной строки
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm") // Желаемый формат
            return try {
                val date = inputFormat.parse(timestamp) // Парсим строку в объект Date
                outputFormat.format(date) // Форматируем дату в нужный формат
            } catch (e: Exception) {
                "Ошибка формата" // Обработка ошибки
            }
        }
        private fun calculatePercentageScore(score: Int, questionCount: Int): Float {
            return if (questionCount > 0) {
                (score.toFloat() / questionCount * 100).toFloat()
            } else {
                0f
            }
        }

        private fun calculateGrade(percentageScore: Float): Int {
            return when {
                percentageScore > 84 -> 5
                percentageScore > 69 -> 4
                percentageScore > 51 -> 3
                else -> 2
            }
        }

        private fun calculateDuration(start: String, end: String?): String {
            // Здесь можно добавить логику для расчета длительности
            // Например, преобразование даты в формат "17 мин. 4 сек."
            return "17 мин. 4 сек." // Замените на реальную логику
        }
    }
}