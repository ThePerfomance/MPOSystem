package com.example.groupprojectfirsttry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        clearBackStack()
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    private fun clearBackStack() {
        (requireActivity() as? SecondActivityWithBottomNavMenu)?.clearBackStack()
    }
}