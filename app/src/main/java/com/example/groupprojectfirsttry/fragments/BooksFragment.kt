package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.R

class BooksFragment : Fragment(R.layout.fragment_books) {

    private lateinit var rLayoutTheoria:RelativeLayout
    private lateinit var rLayoutTests:RelativeLayout
    private lateinit var rLayoutLabWork:RelativeLayout
    private lateinit var rLayoutPractikalWork:RelativeLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rLayoutTheoria = view.findViewById(R.id.relativeLayoutTheoria)
        rLayoutTests = view.findViewById(R.id.relativeLayoutTests)
        rLayoutLabWork = view.findViewById(R.id.relativeLayoutLabWork)
        rLayoutPractikalWork = view.findViewById(R.id.relativeLayoutPractikal)

        rLayoutTheoria.setOnClickListener {
            // Действие при нажатии на RelativeLayout
            openTheoriaFragment()
            Toast.makeText(requireContext(), "Теория на месте!", Toast.LENGTH_SHORT).show()

        }
        rLayoutTests.setOnClickListener {
            // Действие при нажатии на RelativeLayout

            openTestsFragment()
            Toast.makeText(requireContext(), "Тесты на месте!", Toast.LENGTH_SHORT).show()
        }
        rLayoutLabWork.setOnClickListener {
            // Действие при нажатии на RelativeLayout
            Toast.makeText(requireContext(), "Лаб. работы уже тут!", Toast.LENGTH_SHORT).show()
        }
        rLayoutPractikalWork.setOnClickListener {
            // Действие при нажатии на RelativeLayout
            Toast.makeText(requireContext(), "Практика ждёт!", Toast.LENGTH_SHORT).show()
        }

    }
    private fun openTheoriaFragment() {
        val theoriaFragment = TheoriaFragment()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, theoriaFragment) // fragment_container - это ID контейнера для фрагментов
        transaction.addToBackStack(null) // Добавляем в стек назад, чтобы можно было вернуться
        transaction.commit()
    }
    private fun openTestsFragment() {
        val testsFragment = TestsFragment()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, testsFragment) // fragment_container - это ID контейнера для фрагментов
        transaction.addToBackStack(null) // Добавляем в стек назад, чтобы можно было вернуться
        transaction.commit()
    }
}