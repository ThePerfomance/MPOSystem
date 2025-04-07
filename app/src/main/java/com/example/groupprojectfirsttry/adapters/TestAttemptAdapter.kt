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
import kotlin.math.roundToInt

class TestAttemptAdapter(
    private val attempts: List<TestStatistic>,
    private val questionCount: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_test_attempt_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_test_attempt, parent, false)
                AttemptViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                // Ничего не делаем, так как заголовок статичен
            }
            is AttemptViewHolder -> {
                val attempt = attempts[position - 1] // Учитываем, что первая позиция — заголовок
                holder.bind(attempt, questionCount)
            }
        }
    }

    override fun getItemCount(): Int = attempts.size + 1 // +1 для заголовка

    // ViewHolder для заголовка
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    // ViewHolder для обычных элементов
    inner class AttemptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAttemptNumber: TextView = itemView.findViewById(R.id.tvAttemptNumber)
        private val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)
        private val tvEndTime: TextView = itemView.findViewById(R.id.tvEndTime)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)
        private val tvGrade: TextView = itemView.findViewById(R.id.tvGrade)
        private val llVisualStudentStatistic: LinearLayout = itemView.findViewById(R.id.llVisualStudentStatistic)

        fun bind(attempt: TestStatistic, questionCount: Int) {
            tvAttemptNumber.text = (adapterPosition).toString()
            tvStartTime.text = formatTimestamp(attempt.started_at)
            tvEndTime.text = formatTimestamp(attempt.completed_at)
            tvDuration.text = calculateDuration(attempt.started_at, attempt.completed_at)

            // Округление процентов до целых чисел
            val percentageScore = if (questionCount > 0) {
                ((attempt.score.toFloat() / questionCount * 100).toFloat()).roundToInt()
            } else {
                0
            }
            tvScore.text = "$percentageScore%"
            tvGrade.text = calculateGrade(percentageScore.toFloat()).toString()

            // Альтернативный цвет для строк
            if ((adapterPosition) % 2 == 0) {
                llVisualStudentStatistic.setBackgroundColor(itemView.context.getColor(R.color.LightBlueForList))
            } else {
                llVisualStudentStatistic.setBackgroundColor(itemView.context.getColor(android.R.color.transparent))
            }
        }

        private fun formatTimestamp(timestamp: String?): String {
            if (timestamp.isNullOrEmpty()) {
                return "---"
            }
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")
            return try {
                val date = inputFormat.parse(timestamp)
                outputFormat.format(date)
            } catch (e: Exception) {
                "Ошибка формата"
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
            return "17 мин. 4 сек." // Замените на реальную логику
        }
    }
}