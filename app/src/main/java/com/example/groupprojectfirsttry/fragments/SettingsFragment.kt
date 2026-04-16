package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.ThemeManager
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchTrainer = view.findViewById<MaterialSwitch>(R.id.switchTrainer)
        
        // Устанавливаем текущее состояние из настроек
        switchTrainer.isChecked = ThemeManager.isTrainerEnabled(requireContext())
        
        // Слушатель изменения состояния
        switchTrainer.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setTrainerEnabled(requireContext(), isChecked)
        }
    }
}