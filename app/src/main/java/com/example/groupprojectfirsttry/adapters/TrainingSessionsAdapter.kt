package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.TrainingSession
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

class TrainingSessionsAdapter(
    private val sessions: List<TrainingSession>,
    private val onSessionClick: (TrainingSession) -> Unit
) : RecyclerView.Adapter<TrainingSessionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSessionTitle: TextView = view.findViewById(R.id.tvSessionTitle)
        val tvQuestionCount: TextView = view.findViewById(R.id.tvQuestionCount)
        val tvCreatedAt: TextView = view.findViewById(R.id.tvCreatedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        
        // Попытка получить ID теста из вопросов. 
        // Если ID теста 0 или null, выводим просто "Работа над ошибками"
        val testId = session.questions?.firstOrNull()?.question?.test_id
        holder.tvSessionTitle.text = if (testId != null && testId != 0) {
            "Работа над ошибками (Тест #$testId)"
        } else {
            "Работа над ошибками"
        }
        
        val unresolvedCount = session.questions?.count { it.status != "correct" } ?: 0
        holder.tvQuestionCount.text = "$unresolvedCount вопросов для исправления"
        
        holder.tvCreatedAt.text = "Создано: ${formatDate(session.createdAt)}"
        
        holder.itemView.setOnClickListener {
            onSessionClick(session)
        }
    }

    override fun getItemCount() = sessions.size

    private fun formatDate(dateString: String): String {
        return try {
            // Парсим входящую строку как UTC (с учетом разных возможных форматов от Django)
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
            )
            
            var date: Date? = null
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    if (format.contains("Z") || format.contains("'T'")) {
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    date = sdf.parse(dateString)
                    if (date != null) break
                } catch (e: Exception) { continue }
            }

            if (date != null) {
                // Форматируем в локальное время устройства
                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getDefault()
                outputFormat.format(date)
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
}
