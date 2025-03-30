package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R

class PractWorksAdapter(private val practWorks: List<String>) : RecyclerView.Adapter<PractWorksAdapter.PractWorkViewHolder>() {

    // Интерфейс для обработчика кликов
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PractWorkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pract_work_item, parent, false)
        return PractWorkViewHolder(view)
    }

    override fun onBindViewHolder(holder: PractWorkViewHolder, position: Int) {
        holder.bind(practWorks[position])

        // Устанавливаем обработчик кликов на иконку папки
        holder.itemView.findViewById<ImageView>(R.id.folderIcon).setOnClickListener {
            onItemClickListener?.onItemClick(position)
        }
    }

    override fun getItemCount(): Int = practWorks.size

    class PractWorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val practWorkTitle: TextView = itemView.findViewById(R.id.textViewPractWorkTitle)

        fun bind(practWork: String) {
            practWorkTitle.text = practWork
        }
    }
}