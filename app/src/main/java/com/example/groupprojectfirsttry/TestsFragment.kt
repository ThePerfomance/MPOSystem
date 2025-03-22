package com.example.groupprojectfirsttry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class TestsFragment:Fragment(R.layout.fragment_tests) {

    private lateinit var clUpHead: ConstraintLayout
    private lateinit var bnmDown: BottomNavigationView
    private lateinit var testList: RecyclerView
    private lateinit var adapter: TestsAdapter
    private lateinit var user: User // Поле для хранения пользователя

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tests, container, false)
        user = (activity as SecondActivityWithBottomNavMenu?)!!.getUser()
        clUpHead =requireActivity().findViewById(R.id.constraintLayoutUpHead)
        bnmDown=requireActivity().findViewById(R.id.bottom_nav)
        testList = view.findViewById(R.id.testListContainer)
        testList.layoutManager = LinearLayoutManager(context)
        //Устанавливаем фон
        clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_gray_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_gray_background, context?.theme)
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

                // Обновление адаптера
                adapter.updateTests(tests)
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


    }
    override fun onDestroy() {
        super.onDestroy()
        clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
    }

}