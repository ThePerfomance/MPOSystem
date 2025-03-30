package com.example.groupprojectfirsttry.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.fragments.TestStudentResult
import com.example.groupprojectfirsttry.simpleClasses.User

class StudentAdapter(private val students: List<User>) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.student_item, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.bind(student)
    }

    override fun getItemCount(): Int = students.size

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val studentNumber: TextView = itemView.findViewById(R.id.studentNumber)
        private val studentName: TextView = itemView.findViewById(R.id.studentName)

        fun bind(student: User) {
            studentNumber.text = "${adapterPosition + 1}."
            studentName.text = "${student.lastname} ${student.firstname}"

            itemView.setOnClickListener {
                val bundle=Bundle().apply {
                    putSerializable("userId", student.id)
                }
                val fragment = TestStudentResult().apply {
                    arguments = bundle
                }
                (itemView.context as SecondActivityWithBottomNavMenu).replaceFragment(fragment,bundle)
            }
        }
    }
}