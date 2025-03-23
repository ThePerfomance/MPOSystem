package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_answer_result, parent, false)
        return AnswerViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        val answer = answers[position]

        // Отображаем текст ответа
        holder.tvAnswer.text = answer.text

        // Выделяем выбранный ответ
        if (answer.text == selectedAnswerText) {
            holder.tvAnswer.setTextColor(android.graphics.Color.BLUE)
        } else {
            holder.tvAnswer.setTextColor(android.graphics.Color.BLACK)
        }
    }

    override fun getItemCount(): Int = answers.size
}