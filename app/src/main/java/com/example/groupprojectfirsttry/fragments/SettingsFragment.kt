package com.example.groupprojectfirsttry.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.groupprojectfirsttry.BuildConfig
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.ThemeManager
import com.example.groupprojectfirsttry.interfaces.UserProvider

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeSection(view)
        setupFontSection(view)
        updateLabels(view)
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    private fun setupThemeSection(view: View) {
        val llTheme = view.findViewById<LinearLayout>(R.id.llThemeSettings)

        if (ThemeManager.canChangeTheme) {
            llTheme.isEnabled = true
            llTheme.alpha = 1.0f
            llTheme.setOnClickListener { showThemeDialog() }
        } else {
            llTheme.isEnabled = false
            llTheme.alpha = 0.4f
            llTheme.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Смена темы недоступна в этой версии",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showThemeDialog() {
        val ctx = context ?: return
        val options = arrayOf(
            "Изумруд (зелёный)", "Рубин (красный)", "Янтарь (жёлто-оранжевый)",
            "Сапфир (синий)", "Топаз (голубой)", "Аметист (фиолетовый)", "Алмаз (серые)"
        )
        val current = ThemeManager.getSavedThemeIndex(ctx)

        AlertDialog.Builder(ctx)
            .setTitle("Выберите тему")
            .setSingleChoiceItems(options, current) { dialog, which ->
                ThemeManager.saveTheme(ctx, which)
                dialog.dismiss()
                restartWithSettings() // ← перезапуск с открытием настроек
            }
            .show()
    }

    // ─── Font ─────────────────────────────────────────────────────────────────

    private fun setupFontSection(view: View) {
        view.findViewById<LinearLayout>(R.id.llFontSizeSettings)
            .setOnClickListener { showFontSizeDialog() }
    }

    private fun showFontSizeDialog() {
        val ctx = context ?: return
        val options = arrayOf("Крупный (20sp)", "Средний (16sp)", "Мелкий (12sp)")
        val current = ThemeManager.getSavedFontSizeIndex(ctx)

        AlertDialog.Builder(ctx)
            .setTitle("Выберите размер шрифта")
            .setSingleChoiceItems(options, current) { dialog, which ->
                ThemeManager.saveFontSize(ctx, which)
                dialog.dismiss()
                updateLabels(requireView())
            }
            .show()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun updateLabels(view: View) {
        val ctx = context ?: return

        val themeNames = listOf(
            "Изумруд", "Рубин", "Янтарь",
            "Сапфир", "Топаз", "Аметист", "Алмаз"
        )
        val fontNames = listOf("Крупный", "Средний", "Мелкий")

        view.findViewById<TextView>(R.id.tvThemeLabel).text =
            "Тема: ${themeNames[ThemeManager.getSavedThemeIndex(ctx)]}"

        view.findViewById<TextView>(R.id.tvFontSizeLabel).text =
            "Размер шрифта: ${fontNames[ThemeManager.getSavedFontSizeIndex(ctx)]}"
    }

    // Перезапуск Activity с флагом открыть настройки
    private fun restartWithSettings() {
        val activity = requireActivity() as? SecondActivityWithBottomNavMenu ?: return
        val user = (activity as UserProvider).getUser()

        val intent = Intent(activity, SecondActivityWithBottomNavMenu::class.java).apply {
            putExtra("user", user)
            putExtra("open_settings", true) // ← флаг для открытия настроек
            // Очищаем стек активностей чтобы не накапливались
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activity.startActivity(intent)
        activity.finish()
    }
}