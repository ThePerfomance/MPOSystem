package com.example.groupprojectfirsttry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class TestsFragment:Fragment(R.layout.fragment_tests) {

    private lateinit var clUpHead: ConstraintLayout
    private lateinit var bnmDown: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tests, container, false)
        clUpHead = requireActivity().findViewById(R.id.constraintLayoutUpHead) // Ищем элемент внутри текущего фрагмента
        bnmDown=requireActivity().findViewById(R.id.bottom_nav)
        clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_gray_background, context?.theme) // Устанавливаем фон
        bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_gray_background, context?.theme)
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        clUpHead?.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
        bnmDown?.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
    }
}