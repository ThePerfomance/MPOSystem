package com.example.groupprojectfirsttry.fragments

import android.content.Intent
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
import com.facebook.shimmer.ShimmerFrameLayout
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
    
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var profileContent: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        shimmerContainer = view.findViewById(R.id.shimmer_view_container)
        profileContent = view.findViewById(R.id.profile_content)

        tvSurname = view.findViewById(R.id.textViewSurname)
        tvName = view.findViewById(R.id.textViewName)
        tvPatronymic = view.findViewById(R.id.textViewOtchestvo)
        tvGroup = view.findViewById(R.id.textViewGroup)

        etSurname = view.findViewById(R.id.editTextText)
        etName = view.findViewById(R.id.editTextText2)
        etPatronymic = view.findViewById(R.id.editTextText3)
        etGroup = view.findViewById(R.id.editTextText4)
        imgExit = view.findViewById(R.id.imageViewExit)

        tvCenterTitle = requireActivity().findViewById(R.id.textViewUpper)
        tvCenterTitle.text = "Данные профиля"

        // Получаем текущего пользователя из активности
        val activity = requireActivity() as SecondActivityWithBottomNavMenu
        val user = activity.getUser()

        // Заполняем поля данными пользователя
        etSurname.setText(user.lastname)
        etName.setText(user.firstname)
        etPatronymic.setText(user.patronymic)

        // По умолчанию показываем скелетон, если нам нужно что-то загрузить
        if (user.role == "student") {
            startLoading()
            
            tvGroup.visibility = View.VISIBLE
            etGroup.visibility = View.VISIBLE
            imgExit.visibility = View.GONE

            lifecycleScope.launch {
                val groups = activity.getUserGroups()
                if (groups != null && groups.isNotEmpty()) {
                    etGroup.setText(groups.joinToString(", ") { it.name })
                } else {
                    etGroup.setText("Группа не назначена")
                }
                stopLoading()
            }
        } else if (user.role == "teacher") {
            tvGroup.visibility = View.GONE
            etGroup.visibility = View.GONE
            imgExit.visibility = View.VISIBLE
            
            // Для учителя загружать ничего не нужно (в данном фрагменте), 
            // но для единообразия можно показать на мгновение или сразу отобразить контент
            stopLoading()

            imgExit.setOnClickListener {
                Log.d("ProfileFragment", "Кнопка 'Выйти из профиля для преподавателя' нажата")

                val alertDialog = AlertDialog.Builder(requireContext())
                alertDialog.setTitle("Выход из профиля")
                alertDialog.setMessage("Вы уверены, что хотите выйти из профиля?")

                alertDialog.setPositiveButton("Да") { _, _ ->
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }

                alertDialog.setNegativeButton("Нет") { dialog, _ ->
                    dialog.dismiss()
                }

                alertDialog.show()
            }
        }

        return view
    }

    private fun startLoading() {
        shimmerContainer.visibility = View.VISIBLE
        shimmerContainer.startShimmer()
        profileContent.visibility = View.GONE
    }

    private fun stopLoading() {
        shimmerContainer.stopShimmer()
        shimmerContainer.visibility = View.GONE
        profileContent.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        tvCenterTitle.text = ""
    }

    override fun onResume() {
        super.onResume()
        tvCenterTitle.text = "Данные профиля"
        tvCenterTitle.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tvCenterTitle.text = ""
    }
}