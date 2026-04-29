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
import com.example.groupprojectfirsttry.api.ApiClient

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
        val btnSignOut = view.findViewById<View>(R.id.btnSignOut)

        // Обработчик нажатия для "Данные профиля"
        btnProfileData.setOnClickListener {
            (requireActivity() as SecondActivityWithBottomNavMenu).replaceFragment(ProfileFragment())
            Log.d("ProfileAndTestResultsFragment", "Кнопка 'Данные профиля' нажата")
        }

        // Обработчик нажатия для "Выйти из профиля"
        btnSignOut.setOnClickListener {
            Log.d("ProfileAndTestResultsFragment", "Кнопка 'Выйти из профиля' нажата")

            AlertDialog.Builder(requireContext())
                .setTitle("Выход из профиля")
                .setMessage("Вы уверены, что хотите выйти из профиля?")
                .setPositiveButton("Да") { _, _ ->
                    // 1. Очищаем токены, чтобы авто-логин не сработал при следующем запуске
                    ApiClient.getTokenManager()?.clear()
                    
                    // 2. Переходим на MainActivity и очищаем стек
                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Нет", null)
                .show()
        }
    }
}