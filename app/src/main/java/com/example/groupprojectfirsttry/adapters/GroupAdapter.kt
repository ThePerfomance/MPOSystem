package com.example.groupprojectfirsttry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.api.Group

class GroupAdapter(
    var groups: List<Group>,
    private val onItemClickListener: (Group) -> Unit // Интерфейс для обработки кликов
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.group_item, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group, position + 1)

        // Устанавливаем обработчик кликов
        holder.itemView.setOnClickListener {
            onItemClickListener(group)
        }
    }

    override fun getItemCount(): Int = groups.size

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupNumber: TextView = itemView.findViewById(R.id.groupNumber)
        private val groupName: TextView = itemView.findViewById(R.id.groupName)

        fun bind(group: Group, number: Int) {
            groupNumber.text = number.toString()
            groupName.text = group.name
        }
    }

    // Метод для обновления списка групп
    fun updateGroups(newGroups: List<Group>) {
        this.groups = newGroups
        notifyDataSetChanged()
    }
}