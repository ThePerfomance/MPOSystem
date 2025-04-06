package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.simpleClasses.Test
import com.example.groupprojectfirsttry.adapters.TestStatisticAdapter
import com.example.groupprojectfirsttry.simpleClasses.User
import com.example.groupprojectfirsttry.adapters.TestsAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TestStatistic
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TestsFragment : Fragment(R.layout.fragment_tests) {

    private lateinit var clUpHead: ConstraintLayout
    private lateinit var bnmDown: BottomNavigationView
    private lateinit var testList: RecyclerView
    private lateinit var adapter: TestsAdapter
    private lateinit var tvUpperLeftCorner: TextView
    private lateinit var tvUpperCenter: TextView
    private lateinit var ivTestLogo: ImageView
    private lateinit var user: User // Поле для хранения пользователя

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tests, container, false)
        user = (activity as SecondActivityWithBottomNavMenu?)!!.getUser()
        clUpHead = requireActivity().findViewById(R.id.constraintLayoutUpHead)
        bnmDown = requireActivity().findViewById(R.id.bottom_nav)

        tvUpperLeftCorner = requireActivity().findViewById(R.id.textViewLeftUpperCorner)
        tvUpperCenter = requireActivity().findViewById(R.id.textViewUpper)
        ivTestLogo = requireActivity().findViewById(R.id.imageViewTestLogo)

        testList = view.findViewById(R.id.testListContainer)
        testList.layoutManager = LinearLayoutManager(context)


        // Устанавливаем фон and ui
        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)

        tvUpperCenter.visibility=View.GONE
        tvUpperLeftCorner.visibility=View.VISIBLE
        tvUpperLeftCorner.text="Оценка знаний"
        ivTestLogo.visibility=View.VISIBLE

        //
        // Инициализация адаптера
        adapter = TestsAdapter(
            emptyList(), // Список тестов
            onArrowClick = { test -> // Клик по стрелке
                startTest(test)
            },
            onStatisticsClick = { test -> // Клик по "Статистика"
                showStatistics(test)
            }
        )
        val itemDecorator = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divider_item!!)
            ?.let { itemDecorator.setDrawable(it) }
        testList.addItemDecoration(itemDecorator)
        testList.adapter = adapter

        // Загрузка данных при старте фрагмента
        loadTests()

        return view
    }

    private fun loadTests() {
        // Запуск запроса в корутине
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Вызов метода из ApiService
                val tests = ApiClient.apiService.getTests()
                val testResults = user.id?.let { ApiClient.apiService.getUserTestResults(it) }

                // Группировка результатов по идентификатору теста
                val groupedTestResults = testResults?.groupBy { it.test_id } ?: emptyMap()

                // Обновление адаптера
                adapter.updateTests(tests, groupedTestResults)
            } catch (e: Exception) {
                // Обработка ошибки (например, Toast)
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Обработчики кликов (реализуйте их сами)
    private fun startTest(test: Test) {
        // Создайте Bundle с данными
        val args = Bundle().apply {
            putParcelable("test", test)
            putParcelable("user", user) // Передаем пользователя
        }
        // Вызовите метод активности
        (requireActivity() as SecondActivityWithBottomNavMenu).replaceFragment(TestPassFragment(), args)
    }

    private fun showStatistics(test: Test) {
        // Тег для логирования
        val TAG = "ShowStatistics"
        fun formatDate(dateString: String): String {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            return try {
                val date: Date = inputFormat.parse(dateString) ?: Date()
                outputFormat.format(date)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при преобразовании даты: $dateString", e)
                dateString // Возвращаем исходную строку в случае ошибки
            }
        }
        // Запуск запроса в корутине
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Начало выполнения showStatistics для теста: ${test.id}")

                // Вызов метода из ApiService
                val testResults = user.id?.let {
                    Log.d(TAG, "Получение результатов теста для пользователя: $it")
                    ApiClient.apiService.getUserTestResults(it)
                }

                Log.d(TAG, "Полученные результаты теста: $testResults")

                // Фильтруем результаты для конкретного теста
                val filteredResults = testResults?.filter { it.test_id == test.id }
                filteredResults?.forEach{it.completed_at=
                    it.completed_at?.let { it1 -> formatDate(it1) }
                }
                Log.d(TAG, "Отфильтрованные результаты для теста ${test.id}: $filteredResults")

                // Отображение результатов в диалоговом окне
                if (filteredResults != null) {
                    Log.d(TAG, "Отображение результатов в диалоговом окне")
                    showTestResultsDialog(filteredResults)
                } else {
                    Log.w(TAG, "Нет результатов для теста ${test.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Произошла ошибка при получении результатов теста", e)
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showTestResultsDialog(testResults: List<TestStatistic>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_test_statistic, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)

        val rvResults = dialogView.findViewById<RecyclerView>(R.id.rvResults)
        val adapter = TestStatisticAdapter(testResults)
        rvResults.layoutManager = LinearLayoutManager(context)
        rvResults.adapter = adapter

        builder.setPositiveButton("ОК") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
    override fun onPause() {
        super.onPause()
        tvUpperCenter.visibility=View.VISIBLE
        tvUpperLeftCorner.visibility=View.GONE
        tvUpperLeftCorner.text=""
        ivTestLogo.visibility=View.GONE

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
    }

    override fun onResume() {
        super.onResume()
        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)

        tvUpperCenter.visibility=View.GONE
        tvUpperLeftCorner.visibility=View.VISIBLE
        tvUpperLeftCorner.text="Оценка знаний"
        ivTestLogo.visibility=View.VISIBLE

    }
    override fun onDestroy() {
        super.onDestroy()
        tvUpperCenter.visibility=View.VISIBLE
        tvUpperLeftCorner.visibility=View.GONE
        tvUpperLeftCorner.text=""
        ivTestLogo.visibility=View.GONE

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
    }
}