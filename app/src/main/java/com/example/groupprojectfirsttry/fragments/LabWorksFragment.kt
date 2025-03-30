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
            "Лабораторная работа № 4. Программирование Web-сайтов на стороне Web-сервера Apache средствами языка PHP",
            "Лабораторная работа № 5. Создание базы данных в СУБД MySQL"
        )

        adapter = LabWorksAdapter(labWorks)
        recyclerView.adapter = adapter

        // Установка обработчика клика на иконку папки
        adapter.setOnItemClickListener(object : LabWorksAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val selectedLabWork = labWorks[position]
                val fileName = when (selectedLabWork) {
                    "Лабораторная работа № 1. Создание статических Web-страниц с использованием HTML" -> "Lab_rab_1.docx"
                    "Лабораторная работа № 2. Разработка стилизованных веб-сайтов средствами каскадных таблиц стилей (CSS)" -> "Lab_rab_2.docx"
                    "Лабораторная работа № 3. Создание Web-сайтов с включением сценариев на языке JavaScript" -> "Lab_rab_3.docx"
                    "Лабораторная работа № 4. Программирование Web-сайтов на стороне Web-сервера Apache средствами языка PHP" -> "Lab_rab_4.docx"
                    "Лабораторная работа № 5. Создание базы данных в СУБД MySQL" -> "Lab_rab_5.docx"
                    else -> "0Vvedenie.docx" // Файл по умолчанию
                }
                val fileReadFragment = FileReadFragment(fileName)
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, fileReadFragment) // fragment_container - это ID контейнера для фрагментов
                transaction.addToBackStack(null) // Добавляем в стек назад, чтобы можно было вернуться
                transaction.commit()
            }
        })
    }
}