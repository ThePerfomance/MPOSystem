package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.Subject
import java.util.UUID

class SubjectSelectionAdapter(
    private val subjects: List<Subject>,
    private val selectedSubjectId: UUID?,
    private val onSubjectSelected: (Subject) -> Unit
) : RecyclerView.Adapter<SubjectSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvSubjectName)
        val ivSelected: ImageView = view.findViewById(R.id.ivSelected)
        val viewIconBg: View = view.findViewById(R.id.viewSubjectIconBg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subject = subjects[position]
        holder.tvName.text = subject.name
        
        val isSelected = subject.id == selectedSubjectId
        holder.ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
        
        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.bg_answer_item_selector)
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_subject_item_selector)
        }

        holder.itemView.setOnClickListener {
            onSubjectSelected(subject)
        }
    }

    override fun getItemCount() = subjects.size
}
