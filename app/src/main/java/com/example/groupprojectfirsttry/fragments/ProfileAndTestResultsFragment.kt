package com.example.groupprojectfirsttry.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.MainActivity
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu

class ProfileAndTestResultsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_and_test_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Находим кнопки
        val btnProfileData = view.findViewById<View>(R.id.btnProfileData)
        val btnTestResults = view.findViewById<View>(R.id.btnTestResults)
        val btnSignOut=view.findViewById<View>(R.id.btnSignOut)

        // Обработчик нажатия для "Данные профиля"
        btnProfileData.setOnClickListener {
            (requireActivity() as SecondActivityWithBottomNavMenu).replaceFragment(ProfileFragment())

            Log.d("ProfileAndTestResultsFragment", "Кнопка 'Данные профиля' нажата")
        }

        // Обработчик нажатия для "Результаты тестирования"
        btnTestResults.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable("user", (requireActivity() as SecondActivityWithBottomNavMenu).getUser())
            }
            (requireActivity() as SecondActivityWithBottomNavMenu).replaceFragment(TestStudentResult(),bundle)
            Log.d("ProfileAndTestResultsFragment", "Кнопка 'Результаты тестирования' нажата")
        }
        // Обработчик нажатия для "Выйти из профиля"
        btnSignOut.setOnClickListener {
            Log.d("ProfileAndTestResultsFragment", "Кнопка 'Выйти из профиля для преподавателя' нажата")

            // Создаем диалоговое окно с подтверждением выхода
            val alertDialog = AlertDialog.Builder(requireContext())
            alertDialog.setTitle("Выход из профиля")
            alertDialog.setMessage("Вы уверены, что хотите выйти из профиля?")

            // Кнопка "Да"
            alertDialog.setPositiveButton("Да") { _, _ ->
                // Создаем Intent для запуска MainActivity
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish()
            }

            // Кнопка "Нет"
            alertDialog.setNegativeButton("Нет") { dialog, _ ->
                dialog.dismiss() // Закрываем диалог
            }

            // Показываем диалог
            alertDialog.show()
        }
    }
}