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
            openTheoriaFragment()
            Toast.makeText(requireContext(), "Теория на месте!", Toast.LENGTH_SHORT).show()
        }
        rLayoutTests.setOnClickListener {
            openTestsFragment()
            Toast.makeText(requireContext(), "Тесты на месте!", Toast.LENGTH_SHORT).show()
        }
        rLayoutLabWork.setOnClickListener {
            openLabWorksFragment()
            Toast.makeText(requireContext(), "Лаб. работы уже тут!", Toast.LENGTH_SHORT).show()
        }
        rLayoutPractikalWork.setOnClickListener {
            openPractWorksFragment()
            Toast.makeText(requireContext(), "Практика ждёт!", Toast.LENGTH_SHORT).show()
        }

    }
    private fun openTheoriaFragment() {
        val theoriaFragment = TheoriaFragment()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.slide_in_right, // Анимация для входящего фрагмента (слева направо)
            R.anim.slide_out_left, // Анимация для исходящего фрагмента (справа налево)
            R.anim.slide_in_left,  // Анимация для возврата (справа налево)
            R.anim.slide_out_right // Анимация для закрытия (слева направо)
        )
        transaction.replace(R.id.fragment_container, theoriaFragment) // fragment_container - это ID контейнера для фрагментов
        transaction.addToBackStack(null) // Добавляем в стек назад, чтобы можно было вернуться
        transaction.commit()
    }
    private fun openTestsFragment() {
        val testsFragment = TestsFragment()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.slide_in_right, // Анимация для входящего фрагмента (слева направо)
            R.anim.slide_out_left, // Анимация для исходящего фрагмента (справа налево)
            R.anim.slide_in_left,  // Анимация для возврата (справа налево)
            R.anim.slide_out_right // Анимация для закрытия (слева направо)
        )
        transaction.replace(R.id.fragment_container, testsFragment) // fragment_container - это ID контейнера для фрагментов
        transaction.addToBackStack(null) // Добавляем в стек назад, чтобы можно было вернуться
        transaction.commit()
    }
    private fun openLabWorksFragment() {
        val labworksFragment = LabWorksFragment()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.slide_in_right, // Анимация для входящего фрагмента (слева направо)
            R.anim.slide_out_left, // Анимация для исходящего фрагмента (справа налево)
            R.anim.slide_in_left,  // Анимация для возврата (справа налево)
            R.anim.slide_out_right // Анимация для закрытия (слева направо)
        )
        transaction.replace(R.id.fragment_container, labworksFragment) // fragment_container - это ID контейнера для фрагментов
        transaction.addToBackStack(null) // Добавляем в стек назад, чтобы можно было вернуться
        transaction.commit()
    }
    private fun openPractWorksFragment() {
        val practWorksFragment = PractWorksFragment()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.slide_in_right, // Анимация для входящего фрагмента (слева направо)
            R.anim.slide_out_left, // Анимация для исходящего фрагмента (справа налево)
            R.anim.slide_in_left,  // Анимация для возврата (справа налево)
            R.anim.slide_out_right // Анимация для закрытия (слева направо)
        )
        transaction.replace(R.id.fragment_container, practWorksFragment) // fragment_container - это ID контейнера для фрагментов
        transaction.addToBackStack(null) // Добавляем в стек назад, чтобы можно было вернуться
        transaction.commit()
    }
}