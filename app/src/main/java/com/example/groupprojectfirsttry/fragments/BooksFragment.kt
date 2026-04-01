package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.BuildConfig
import com.example.groupprojectfirsttry.R

class BooksFragment : Fragment(R.layout.fragment_books) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настройка видимости в зависимости от flavor
        val isImpuls = BuildConfig.FLAVOR == "impuls"

        view.findViewById<RelativeLayout>(R.id.relativeLayoutLabWork).apply {
            visibility = if (isImpuls) View.GONE else View.VISIBLE
        }
        view.findViewById<RelativeLayout>(R.id.relativeLayoutPractikal).apply {
            visibility = if (isImpuls) View.GONE else View.VISIBLE
        }

        // Клики
        view.findViewById<RelativeLayout>(R.id.relativeLayoutTheoria)
            .setOnClickListener { navigateTo(TheoriaFragment(), "Теория на месте!") }

        view.findViewById<RelativeLayout>(R.id.relativeLayoutTests)
            .setOnClickListener { navigateTo(TestsFragment(), "Тесты на месте!") }

        if (!isImpuls) {
            view.findViewById<RelativeLayout>(R.id.relativeLayoutLabWork)
                .setOnClickListener { navigateTo(LabWorksFragment(), "Лаб. работы уже тут!") }

            view.findViewById<RelativeLayout>(R.id.relativeLayoutPractikal)
                .setOnClickListener { navigateTo(PractWorksFragment(), "Практика ждёт!") }
        }
    }

    private fun navigateTo(fragment: Fragment, toastMessage: String) {
        android.widget.Toast.makeText(requireContext(), toastMessage, android.widget.Toast.LENGTH_SHORT).show()

        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}