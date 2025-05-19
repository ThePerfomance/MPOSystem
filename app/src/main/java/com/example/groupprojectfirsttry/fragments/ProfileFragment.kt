package com.example.groupprojectfirsttry.fragments

import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.MainActivity
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import kotlinx.coroutines.*

class ProfileFragment : Fragment() {
    private lateinit var tvSurname: TextView
    private lateinit var tvName: TextView
    private lateinit var tvPatronymic: TextView
    private lateinit var tvGroup: TextView
    private lateinit var etSurname: EditText
    private lateinit var etName: EditText
    private lateinit var etPatronymic: EditText
    private lateinit var etGroup: EditText
    private lateinit var tvCenterTitle: TextView
    private lateinit var imgExit: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvSurname = view.findViewById(R.id.textViewSurname)
        tvName = view.findViewById(R.id.textViewName)
        tvPatronymic = view.findViewById(R.id.textViewOtchestvo)
        tvGroup = view.findViewById(R.id.textViewGroup)

        etSurname = view.findViewById(R.id.editTextText)
        etName = view.findViewById(R.id.editTextText2)
        etPatronymic = view.findViewById(R.id.editTextText3)
        etGroup = view.findViewById(R.id.editTextText4)
        imgExit=view.findViewById(R.id.imageViewExit)

        tvCenterTitle=requireActivity().findViewById(R.id.textViewUpper)
        tvCenterTitle.text="Данные профиля"

        // Получаем текущего пользователя из активности
        val activity = requireActivity() as SecondActivityWithBottomNavMenu
        val user = activity.getUser()

        // Заполняем поля данными пользователя
        etSurname.setText(user.lastname)
        etName.setText(user.firstname)
        etPatronymic.setText(user.patronymic)

        // Получаем группы пользователя и заполняем поле группы
        when(user.role)
        {
            "student"->
            {
                tvGroup.visibility=View.VISIBLE
                etGroup.visibility=View.VISIBLE
                imgExit.visibility=View.GONE

                lifecycleScope.launch {
                    val groups = activity.getUserGroups()
                    if (groups != null && groups.isNotEmpty()) {
                        etGroup.setText(groups.joinToString(", ") { it.name })
                    } else {

                        etGroup.setText("Группа не назначена")
                    }
                }
            }
            "teacher"->{
                tvGroup.visibility=View.GONE
                etGroup.visibility=View.GONE
                imgExit.visibility=View.VISIBLE

                imgExit.setOnClickListener {
                    Log.d("ProfileFragment", "Кнопка 'Выйти из профиля для преподавателя' нажата")

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


        return view
    }
    override fun onPause() {
        super.onPause()
        tvCenterTitle.text=""
    }
    override fun onResume() {
        super.onResume()
        tvCenterTitle.text="Данные профиля"
        tvCenterTitle.visibility=View.VISIBLE

    }
    override fun onDestroyView() {
        super.onDestroyView()
        tvCenterTitle.text=""
    }
}