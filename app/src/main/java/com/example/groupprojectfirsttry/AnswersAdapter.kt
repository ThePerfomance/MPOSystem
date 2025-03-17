package com.example.groupprojectfirsttry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AnswersAdapter(
    private val answers: List<Answer>,
    private val onAnswerSelected: (Answer) -> Unit
) : RecyclerView.Adapter<AnswersAdapter.AnswerViewHolder>() {

    class AnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAnswer: TextView = itemView.findViewById(R.id.tvAnswer)
        val cbAnswer: CheckBox = itemView.findViewById(R.id.cbAnswer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_answer, parent, false)
        return AnswerViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        val answer = answers[position]
        holder.tvAnswer.text = answer.text
        holder.cbAnswer.isChecked = answer.isSelected

        holder.itemView.setOnClickListener {
            answer.isSelected = !answer.isSelected
            onAnswerSelected(answer)
        }
    }

    override fun getItemCount() = answers.size
}