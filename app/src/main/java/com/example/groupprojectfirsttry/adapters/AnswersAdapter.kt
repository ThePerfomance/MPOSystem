package com.example.groupprojectfirsttry.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.simpleClasses.Answer
import com.example.groupprojectfirsttry.R

class AnswersAdapter(
    private val answers: List<Answer>,
    initialSelectedAnswer: Answer? = null,
    private val onAnswerSelected: (Answer) -> Unit
) : RecyclerView.Adapter<AnswersAdapter.AnswersViewHolder>() {

    private var selectedAnswer: Answer? = initialSelectedAnswer

    class AnswersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radioButton: RadioButton = itemView.findViewById(R.id.radioButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswersViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_answer, parent, false)
        return AnswersViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnswersViewHolder, position: Int) {
        val answer = answers[position]
        holder.radioButton.text = answer.text
        holder.radioButton.isChecked = answer.id == selectedAnswer?.id

        // Force red color for the RadioButton in code to bypass theme
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                Color.parseColor("#FF0000"), // Checked - RED
                Color.parseColor("#757575")  // Unchecked - GRAY
            )
        )
        holder.radioButton.buttonTintList = colorStateList

        holder.radioButton.setOnClickListener {
            selectedAnswer = answer
            onAnswerSelected(answer)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = answers.size

    fun getSelectedAnswer(): Answer? = selectedAnswer
}