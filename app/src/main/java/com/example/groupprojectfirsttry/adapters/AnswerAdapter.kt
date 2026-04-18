package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.simpleClasses.Answer
import com.example.groupprojectfirsttry.R

class AnswerAdapter(
    private val answers: List<Answer>,
    private val selectedAnswerText: String
) : RecyclerView.Adapter<AnswerAdapter.AnswerViewHolder>() {

    class AnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAnswer: TextView = itemView.findViewById(R.id.tvAnswer)
        val llAnswerContainer: View = itemView.findViewById(R.id.llAnswerContainer)
        val ivCheck: ImageView = itemView.findViewById(R.id.ivCheck)
        val vIndicator: View = itemView.findViewById(R.id.vIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_answer_result, parent, false)
        return AnswerViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        val answer = answers[position]
        holder.tvAnswer.text = answer.text

        // Согласно инструкции: показываем только выбранный ответ, 
        // но НЕ сообщаем пользователю, правильный он или нет (в списке вопросов).
        // Поэтому используем нейтральное выделение или как на фото (если там выбранный).
        
        if (answer.text == selectedAnswerText) {
            // Выбранный ответ - выделяем рамкой и фоном (как на фото, но нейтрально)
            holder.llAnswerContainer.setBackgroundResource(R.drawable.bg_answer_selected)
            holder.ivCheck.visibility = View.VISIBLE
            holder.vIndicator.visibility = View.VISIBLE
            holder.tvAnswer.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            // Обычный ответ
            holder.llAnswerContainer.setBackgroundResource(R.drawable.bg_answer_default)
            holder.ivCheck.visibility = View.GONE
            holder.vIndicator.visibility = View.GONE
            holder.tvAnswer.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    override fun getItemCount(): Int = answers.size
}
