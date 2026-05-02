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
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

// УБРАЛИ параметр questionCount из конструктора
class TestAttemptAdapter(
    private val attempts: List<TestStatistic>
) : RecyclerView.Adapter<TestAttemptAdapter.AttemptViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttemptViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_test_attempt, parent, false)
        return AttemptViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttemptViewHolder, position: Int) {
        holder.bind(attempts[position], position + 1)
    }

    override fun getItemCount(): Int = attempts.size

    inner class AttemptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAttemptNumber: TextView = itemView.findViewById(R.id.tvAttemptNumber)
        private val tvEndTime: TextView = itemView.findViewById(R.id.tvEndTime)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)
        private val tvGrade: TextView = itemView.findViewById(R.id.tvGrade)
        private val llVisualStudentStatistic: LinearLayout = itemView.findViewById(R.id.llVisualStudentStatistic)

        fun bind(attempt: TestStatistic, number: Int) {
            tvAttemptNumber.text = number.toString()
            tvEndTime.text = formatTimestamp(attempt.completed_at)
            tvDuration.text = calculateDuration(attempt.started_at, attempt.completed_at)

            // 1. Берем набранные баллы и максимум баллов
            val earned = attempt.score // (или attempt.earnedPoints)
            val total = attempt.totalPoints

            // 2. Считаем правильный процент для оценки
            val percentageScore = if (total > 0) {
                (earned.toDouble() / total * 100).toInt()
            } else {
                0
            }

            // 3. Красиво выводим баллы в формате "Набрал / Максимум"
            tvScore.text = "$earned / $total"

            // 4. Оценка теперь считается правильно на основе процентов
            tvGrade.text = when {
                percentageScore > 84 -> 5
                percentageScore > 69 -> 4
                percentageScore > 51 -> 3
                else -> 2
            }.toString()

            // Зебра-раскраска
            if (number % 2 == 0) {
                llVisualStudentStatistic.setBackgroundColor(itemView.context.getColor(R.color.AppBackgroundColor))
            } else {
                llVisualStudentStatistic.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }

        private fun formatTimestamp(timestamp: String?): String {
            if (timestamp.isNullOrEmpty()) return "---"
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
            )
            for (f in formats) {
                try {
                    val sdf = SimpleDateFormat(f, Locale.getDefault())
                    if (f.contains("Z") || f.contains("'T'")) {
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val date = sdf.parse(timestamp) ?: continue

                    val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    outputFormat.timeZone = TimeZone.getDefault()
                    return outputFormat.format(date)
                } catch (e: Exception) { continue }
            }
            return timestamp
        }

        private fun calculateDuration(start: String?, end: String?): String {
            if (start.isNullOrEmpty() || end.isNullOrEmpty()) return "---"

            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
            )
            var startDate: java.util.Date? = null
            var endDate: java.util.Date? = null

            for (f in formats) {
                val sdf = SimpleDateFormat(f, Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
                try { if (startDate == null) startDate = sdf.parse(start) } catch (e: Exception) {}
                try { if (endDate == null) endDate = sdf.parse(end) } catch (e: Exception) {}
            }

            if (startDate == null || endDate == null) return "---"

            var diff = abs(endDate.time - startDate.time)

            val s = (diff / 1000) % 60
            val m = (diff / 60000) % 60
            val h = diff / 3600000

            return when {
                h > 0 -> String.format("%d ч. %d мин.", h, m)
                m > 0 -> String.format("%d мин. %d сек.", m, s)
                else -> String.format("%d сек.", s)
            }
        }
    }
}