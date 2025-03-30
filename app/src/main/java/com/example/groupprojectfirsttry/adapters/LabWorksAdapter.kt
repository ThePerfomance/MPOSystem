package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R

class LabWorksAdapter(private val labWorks: List<String>) : RecyclerView.Adapter<LabWorksAdapter.LabWorkViewHolder>() {

    // Интерфейс для обработчика кликов
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabWorkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.lab_work_item, parent, false)
        return LabWorkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LabWorkViewHolder, position: Int) {
        holder.bind(labWorks[position])

        // Устанавливаем обработчик кликов на иконку папки
        holder.itemView.findViewById<ImageView>(R.id.folderIcon).setOnClickListener {
            onItemClickListener?.onItemClick(position)
        }
    }

    override fun getItemCount(): Int = labWorks.size

    class LabWorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labWorkTitle: TextView = itemView.findViewById(R.id.textViewLabWorkTitle)

        fun bind(labWork: String) {
            labWorkTitle.text = labWork
        }
    }
}