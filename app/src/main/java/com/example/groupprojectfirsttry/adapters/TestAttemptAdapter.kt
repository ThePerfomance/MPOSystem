package com.example.groupprojectfirsttry.adapters

import android.util.Log
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
import kotlin.math.roundToInt

class TestAttemptAdapter(
    private val attempts: List<TestStatistic>,
    private val questionCount: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int = if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_test_attempt_header, parent, false))
            else -> AttemptViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_test_attempt, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AttemptViewHolder) {
            holder.bind(attempts[position - 1])
        }
    }

    override fun getItemCount(): Int = attempts.size + 1

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class AttemptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAttemptNumber: TextView = itemView.findViewById(R.id.tvAttemptNumber)
        private val tvEndTime: TextView = itemView.findViewById(R.id.tvEndTime)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)
        private val tvGrade: TextView = itemView.findViewById(R.id.tvGrade)
        private val llVisualStudentStatistic: LinearLayout = itemView.findViewById(R.id.llVisualStudentStatistic)

        fun bind(attempt: TestStatistic) {
            tvAttemptNumber.text = adapterPosition.toString()
            tvEndTime.text = formatTimestamp(attempt.completed_at)
            tvDuration.text = calculateDuration(attempt.started_at, attempt.completed_at)

            // В базе данных score УЖЕ хранится как процент (0-100)
            val percentageScore = attempt.score
            
            tvScore.text = "$percentageScore%"
            tvGrade.text = when {
                percentageScore > 84 -> 5
                percentageScore > 69 -> 4
                percentageScore > 51 -> 3
                else -> 2
            }.toString()

            val bgColor = if (adapterPosition % 2 == 0) R.color.MainColor else android.R.color.transparent
            llVisualStudentStatistic.setBackgroundColor(itemView.context.getColor(bgColor))
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

            var diff = endDate.time - startDate.time
            
            // Если разница отрицательная, берем абсолютное значение (защита от багов времени)
            diff = abs(diff)

            val s = (diff / 1000) % 60
            val m = (diff / 60000) % 60
            val h = diff / 3600000

            return when {
                h > 0 -> String.format("%d ч. %d мин. %d сек.", h, m, s)
                m > 0 -> String.format("%d мин. %d сек.", m, s)
                else -> String.format("%d сек.", s)
            }
        }
    }
}
