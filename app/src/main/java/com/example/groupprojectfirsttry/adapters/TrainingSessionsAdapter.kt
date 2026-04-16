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
        
        // derivation of a title - since we don't have the test title directly in the model, 
        // we can use the date or a generic "Training" title. 
        // If question details are available, we could potentially get test info there.
        val testId = session.questions?.firstOrNull()?.question?.test_id
        holder.tvSessionTitle.text = if (testId != null) "Работа над ошибками (Тест #$testId)" else "Работа над ошибками"
        
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
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) outputFormat.format(date) else dateString
        } catch (e: Exception) {
            dateString
        }
    }
}
