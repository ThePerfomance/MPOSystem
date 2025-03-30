package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.adapters.PractWorksAdapter

class PractWorksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PractWorksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pract_works, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewPractWorks)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Данные для списка лабораторных работ
        val practWorks = listOf(
            "Практическая работа № 1. Создание веб-приложений с помощью конструкторов сайтов",
            "Практическая работа № 2. Управление содержимым сайтов средствами СMS (Content Management System)",
            "Практическая работа № 3. Применение расширяемого языка разметки XML при разработке веб-страниц",
            "Практическая работа № 4. Создание веб-приложений  с использованием асинхронного подхода к построению интерактивных  страниц - AJAX",
            "Лабораторная работа № 5. Использование языка написания сценариев PHP для работы с многомерными и ассоциативными массивами"
        )
        adapter = PractWorksAdapter(practWorks)
        recyclerView.adapter = adapter

        // Установка обработчика клика на иконку папки
        adapter.setOnItemClickListener(object : PractWorksAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val selectedPractWork = practWorks[position]
                val fileName = when (selectedPractWork) {
                    "Практическая работа № 1. Создание веб-приложений с помощью конструкторов сайтов" -> "Pract_rab_1.docx"
                    "Практическая работа № 2. Управление содержимым сайтов средствами СMS (Content Management System)" -> "Pract_rab_2.docx"
                    "Практическая работа № 3. Применение расширяемого языка разметки XML при разработке веб-страниц" -> "Pract_rab_3.docx"
                    "Практическая работа № 4. Создание веб-приложений  с использованием асинхронного подхода к построению интерактивных  страниц - AJAX" -> "Pract_rab_4.docx"
                    "Лабораторная работа № 5. Использование языка написания сценариев PHP для работы с многомерными и ассоциативными массивами" -> "Pract_rab_5.docx"
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