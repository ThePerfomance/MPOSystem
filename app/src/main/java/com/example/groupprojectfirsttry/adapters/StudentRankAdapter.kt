package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.StudentData
import com.example.groupprojectfirsttry.simpleClasses.User

class StudentRankAdapter(
    private val studentList: List<Pair<User, Pair<StudentData, String>>>
) : RecyclerView.Adapter<StudentRankAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.textViewStudentName)
        val tvRank: TextView = itemView.findViewById(R.id.textViewStudentRank)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_rank, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = studentList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (user, dataWithRank) = studentList[position]
        val (studentData, rank) = dataWithRank

        holder.tvName.text = "${user.lastname} ${user.firstname}"
        holder.tvRank.text = "Ранг: $rank"
    }
}