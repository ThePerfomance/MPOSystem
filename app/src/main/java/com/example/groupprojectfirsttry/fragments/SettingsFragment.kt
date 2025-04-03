package com.example.groupprojectfirsttry.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.R

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        // Обработчик клика по теме
        view.findViewById<LinearLayout>(R.id.llThemeSettings).setOnClickListener {
            showThemeSelectionDialog()
        }

        // Обработчик клика по размеру шрифта
        view.findViewById<LinearLayout>(R.id.llFontSizeSettings).setOnClickListener {
            showFontSizeSelectionDialog()
        }
    }

    private fun showThemeSelectionDialog() {
        val themeOptions = arrayOf("Изумруд (зелёный)", "Рубин (красный)", "Янтарь (желто-оранжевый)", "Сапфир (синий)", "Топаз (голубой)", "Аметист (фиолетовый)", "Алмаз (серые оттенки)")
        val currentThemeIndex = sharedPreferences.getInt("selected_theme", 0)

        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle("Выберите тему")
                .setSingleChoiceItems(themeOptions, currentThemeIndex) { dialog, which ->
                    sharedPreferences.edit().putInt("selected_theme", which).apply()
                    applyTheme(which)
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun showFontSizeSelectionDialog() {
        val fontSizeOptions = arrayOf("Крупный", "Средний", "Мелкий")
        val currentFontSizeIndex = sharedPreferences.getInt("selected_font_size", 1)

        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle("Выберите размер шрифта")
                .setSingleChoiceItems(fontSizeOptions, currentFontSizeIndex) { dialog, which ->
                    sharedPreferences.edit().putInt("selected_font_size", which).apply()
                    applyFontSize(which)
                    dialog.dismiss()
                }
                .show()
        }
    }

    // Применение выбранной темы
    private fun applyTheme(themeIndex: Int) {
//        when (themeIndex) {
//            0 -> setTheme(R.style.Theme_Emerald) // Изумруд
//            1 -> setTheme(R.style.Theme_Ruby) // Рубин
//            2 -> setTheme(R.style.Theme_Amber) // Янтарь
//            3 -> setTheme(R.style.Theme_Sapphire) // Сапфир
//            4 -> setTheme(R.style.Theme_Topaz) // Топаз
//            5 -> setTheme(R.style.Theme_Amethyst) // Аметист
//            6 -> setTheme(R.style.Theme_Diamond) // Алмаз
//        }
//        activity?.recreate() // Пересоздание активности для применения темы
    }

    // Применение выбранного размера шрифта
    private fun applyFontSize(fontSizeIndex: Int) {
//        when (fontSizeIndex) {
//            0 -> setFontSize(R.dimen.large_text_size) // Крупный
//            1 -> setFontSize(R.dimen.medium_text_size) // Средний
//            2 -> setFontSize(R.dimen.small_text_size) // Мелкий
//        }
    }

    // Установка размера шрифта через ресурсы
    private fun setFontSize(sizeResId: Int) {
        // Здесь можно применить размер шрифта глобально или к конкретным элементам
    }
}