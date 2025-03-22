package com.example.groupprojectfirsttry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ResultAdapter(private val results: List<ResultItem>) : RecyclerView.Adapter<ResultAdapter.ResultViewHolder>() {

    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestion: TextView = itemView.findViewById(R.id.tvQuestion)
        val tvSelectedAnswer: TextView = itemView.findViewById(R.id.tvSelectedAnswer)
        val tvCorrectAnswer: TextView = itemView.findViewById(R.id.tvCorrectAnswer)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = results[position]
        holder.tvQuestion.text = result.questionText
        holder.tvSelectedAnswer.text = "Ваш ответ: ${result.selectedAnswerText}"
        holder.tvCorrectAnswer.text = "Правильный ответ: ${result.correctAnswerText}"
        holder.tvStatus.text = if (result.isCorrect) "Правильно" else "Неправильно"
        holder.tvStatus.setTextColor(if (result.isCorrect) android.graphics.Color.GREEN else android.graphics.Color.RED)
    }

    override fun getItemCount(): Int = results.size
}