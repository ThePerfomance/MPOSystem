package com.example.groupprojectfirsttry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AnswersAdapter(
    private val answers: List<Answer>,
    private val onAnswerSelected: (Answer) -> Unit
) : RecyclerView.Adapter<AnswersAdapter.AnswersViewHolder>() {

    private var selectedAnswer: Answer? = null

    class AnswersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radioGroup: RadioGroup = itemView.findViewById(R.id.radioGroupAnswers)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswersViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_answer, parent, false)
        return AnswersViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnswersViewHolder, position: Int) {
        holder.radioGroup.removeAllViews() // Очищаем предыдущие RadioButton

        for ((index, answer) in answers.withIndex()) {
            val radioButton = RadioButton(holder.itemView.context).apply {
                id = View.generateViewId() // Генерируем уникальный ID
                text = answer.text
                isChecked = answer == selectedAnswer
            }

            radioButton.setOnClickListener {
                selectedAnswer = answer
                onAnswerSelected(answer)
            }

            holder.radioGroup.addView(radioButton)
        }
    }

    override fun getItemCount() = 1 // Один элемент для всех ответов
}