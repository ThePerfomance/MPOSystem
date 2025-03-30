package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.adapters.LabWorksAdapter

class LabWorksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LabWorksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_lab_works, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewLabWorks)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Данные для списка лабораторных работ
        val labWorks = listOf(
            "Лабораторная работа № 1. Создание статических Web-страниц с использованием HTML",
            "Лабораторная работа № 2. Разработка стилизованных веб-сайтов средствами каскадных таблиц стилей (CSS)",
            "Лабораторная работа № 3. Создание Web-сайтов с включением сценариев на языке JavaScript",
            "Лабораторная работа № 4. Программирование Web-сайтов на стороне Web-сервера Apache средствами языка PHP"
        )

        adapter = LabWorksAdapter(labWorks)
        recyclerView.adapter = adapter

        // Установка обработчика клика на иконку папки
        adapter.setOnItemClickListener(object : LabWorksAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val selectedLabWork = labWorks[position]
                // Например, открываем Toast или переходим на другую страницу
                // Toast.makeText(requireContext(), "Clicked on $selectedLabWork", Toast.LENGTH_SHORT).show()
            }
        })
    }
}