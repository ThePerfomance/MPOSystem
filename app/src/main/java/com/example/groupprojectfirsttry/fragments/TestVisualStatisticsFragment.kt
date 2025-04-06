package com.example.groupprojectfirsttry.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TestStatistic
import com.example.groupprojectfirsttry.simpleClasses.User
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch

class TestVisualStatisticsFragment : Fragment() {

    private lateinit var tvThemeHeader: TextView
    private lateinit var tvStudentName: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_visual_test_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Находим TextView для заголовка теста и ФИО студента
        tvThemeHeader = view.findViewById(R.id.testThemeHeader)
        tvStudentName = view.findViewById(R.id.studentName)

        // Получаем данные из аргументов
        val testName = requireArguments().getString("testName") ?: "Неизвестный тест"
        val student = requireArguments().getParcelable<User>("student")
        val studentName = "${student?.lastname} ${student?.firstname}" ?: "Неизвестный студент"


        // Устанавливаем заголовок теста и ФИО студента
        tvThemeHeader.text = testName
        tvStudentName.text = studentName

        // Получаем список результатов теста
        val testStatistics =
            requireArguments().getParcelableArrayList<TestStatistic>("testStatistics")
                ?: emptyList()
        if (testStatistics.isNotEmpty())
        {
            val testId=testStatistics[0].test_id
            lifecycleScope.launch {
                try {
                    val questions = ApiClient.apiService.getQuestions(testId)
                    val questionCount = questions.size

                    if (questionCount <= 0) {
                        Log.e("TestVisualStatisticsFragment", "Количество вопросов не указано или равно 0")
                        return@launch
                    }

                    // Находим BarChart в разметке
                    val barChart: BarChart = view.findViewById(R.id.barChart)

                    // Создаем список данных для диаграммы
                    val entries = mutableListOf<BarEntry>()
                    for ((index, statistic) in testStatistics.withIndex()) {
                        // Переводим оценку в проценты
                        val percentageScore = if (statistic.score > 0 && questionCount > 0) {
                            (statistic.score.toFloat() / questionCount * 100).toFloat()
                        } else {
                            0f
                        }
                        entries.add(BarEntry(index.toFloat(), percentageScore))
                    }

                    // Создаем набор данных для диаграммы
                    val dataSet = BarDataSet(entries, "Результаты теста").apply {
                        color = resources.getColor(R.color.blue) // Цвет столбцов
                        valueTextColor = Color.BLACK // Цвет текста значений над столбцами
                        valueTextSize = 12f // Размер текста значений
                    }

                    // Создаем объект BarData и устанавливаем его в диаграмму
                    val barData = BarData(dataSet).apply {
                        barWidth = 0.5f // Ширина столбцов
                        setValueTextSize(12f) // Размер текста значений
                    }

                    barChart.apply {
                        data = barData

                        // Настройка осей
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM // Ось X внизу
                            granularity = 1f // Минимальный шаг между значениями на оси X
                            setDrawGridLines(false) // Убрать сетку по оси X
                            textColor = Color.BLACK // Цвет текста на оси X
                            textSize = 12f // Размер текста на оси X

                            // Подписи для оси X (например, номера попыток)
                            valueFormatter =
                                IndexAxisValueFormatter(testStatistics.mapIndexed { index, _ -> "${index + 1}" })
                        }

                        axisLeft.apply {
                            axisMinimum = 0f // Минимальное значение на оси Y
                            axisMaximum = 100f // Максимальное значение на оси Y (100%)
                            granularity = 10f // Шаг между значениями на оси Y
                            setDrawGridLines(true) // Включить сетку по оси Y
                            textColor = Color.BLACK // Цвет текста на оси Y
                            textSize = 12f // Размер текста на оси Y
                        }

                        axisRight.apply {
                            isEnabled = false // Отключить правую ось Y
                        }

                        // Настройка легенды
                        legend.apply {
                            isEnabled = true // Включить легенду
                            textSize = 14f // Размер текста легенды
                            textColor = Color.BLACK // Цвет текста легенды
                            verticalAlignment =
                                com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP // Выравнивание по вертикали
                            horizontalAlignment =
                                com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER // Выравнивание по горизонтали
                            orientation =
                                com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL // Горизонтальная ориентация
                        }

                        // Настройка взаимодействия
                        description.isEnabled = false // Отключить описание диаграммы
                        setTouchEnabled(true) // Включить взаимодействие с диаграммой
                        isDragEnabled = false // Запретить перетаскивание
                        isScaleXEnabled = false // Запретить масштабирование по оси X
                        isScaleYEnabled = false // Запретить масштабирование по оси Y
                        setPinchZoom(false) // Отключить зум пальцами
                        setDrawBarShadow(false) // Отключить тень за столбцами
                        setDrawValueAboveBar(true) // Показывать значения над столбцами
                        animateY(1000) // Анимация появления столбцов (1 секунда)
                    }

                    // Обновляем диаграмму
                    barChart.invalidate()
                } catch (e: Exception) {
                    Log.e("TestVisualStatisticsFragment", "Ошибка при получении вопросов: ${e.message}")
                }
            }
        }
        // Запрос к серверу для получения количества вопросов в тесте
    }
}