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
        val CardViewItemResult: CardView =itemView.findViewById(R.id.CardViewItemResult)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = results[position]

        // Отображаем текст вопроса
        holder.tvQuestion.text = result.questionText

        // Отображаем номер вопроса
        holder.tvQuestionNumber.text = "${position + 1} / ${results.size}"

        // Настройка RecyclerView для ответов
        val answerAdapter = AnswerAdapter(result.answers, result.selectedAnswerText)
        holder.answersList.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.answersList.adapter = answerAdapter

        // Отображаем статус
        //holder.tvStatus.text = if (result.isCorrect) "Правильно" else "Неправильно"
        //holder.tvStatus.setTextColor(if (result.isCorrect) android.graphics.Color.GREEN else android.graphics.Color.RED)

        holder.CardViewItemResult.setBackgroundResource(if (result.isCorrect) R.color.GraphicCorrectColor else R.color.GraphicInCorrectColor)
    }

    override fun getItemCount(): Int = results.size
}