package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.ResultItem

class ResultAdapter(private val results: List<ResultItem>) : RecyclerView.Adapter<ResultAdapter.ResultViewHolder>() {

    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestion: TextView = itemView.findViewById(R.id.tvQuestion)
        val answersList: RecyclerView = itemView.findViewById(R.id.answersList)
        val tvQuestionNumber: TextView = itemView.findViewById(R.id.tvQuestionNumber)
        val cardView: CardView = itemView.findViewById(R.id.CardViewItemResult)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = results[position]

        // Отображаем номер и текст вопроса (как на фото: "2. Какая главная миссия...")
        holder.tvQuestion.text = "${position + 1}. ${result.questionText}"
        holder.tvQuestionNumber.text = "" // Убираем старый формат номера "1/10"

        // Настройка списка ответов
        val answerAdapter = AnswerAdapter(result.answers, result.selectedAnswerText)
        holder.answersList.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.answersList.adapter = answerAdapter

        // Убираем раскраску всей карточки в красный/зеленый (согласно инструкции)
        holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
        holder.cardView.cardElevation = 0f // Делаем дизайн более плоским как на фото
    }

    override fun getItemCount(): Int = results.size
}
