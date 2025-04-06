            package com.example.groupprojectfirsttry.adapters

            import android.view.LayoutInflater
            import android.view.View
            import android.view.ViewGroup
            import android.widget.TextView
            import androidx.recyclerview.widget.RecyclerView
            import com.example.groupprojectfirsttry.R
            import com.example.groupprojectfirsttry.api.TestStatistic

            class TestStudentResultAdapter(
                private val testStatistics: List<TestStatistic>,
                private val allTestStatistics: List<TestStatistic>,
                private val testQuestionCounts: Map<Int, Int>,
                private val testNames: Map<Int, String>, // Новый параметр: имена тестов
                private val onStatisticsClickListener: OnStatisticsClickListener
            ) : RecyclerView.Adapter<TestStudentResultAdapter.TestResultViewHolder>() {

                // Фильтруем список, чтобы оставить только уникальные test_id
                private val uniqueTestStatistics = testStatistics.distinctBy { it.test_id }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestResultViewHolder {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.test_student_result_item, parent, false)
                    return TestResultViewHolder(view, allTestStatistics, testQuestionCounts, testNames, onStatisticsClickListener)
                }

                override fun onBindViewHolder(holder: TestResultViewHolder, position: Int) {
                    val testStatistic = uniqueTestStatistics[position]
                    holder.bind(testStatistic)
                }

                override fun getItemCount(): Int = uniqueTestStatistics.size

                class TestResultViewHolder(
                    itemView: View,
                    private val allTestStatistics: List<TestStatistic>,
                    private val testQuestionCounts: Map<Int, Int>,
                    private val testNames: Map<Int, String>, // Новый параметр: имена тестов
                    private val onStatisticsClickListener: OnStatisticsClickListener
                ) : RecyclerView.ViewHolder(itemView) {

                    private val testName: TextView = itemView.findViewById(R.id.testName)
                    private val attemptsCount: TextView = itemView.findViewById(R.id.attemptsCount)
                    private val bestScore: TextView = itemView.findViewById(R.id.bestScore)
                    private val statisticsLink: TextView = itemView.findViewById(R.id.statisticsLink)

                    fun bind(testStatistic: TestStatistic) {
                        // Получаем имя теста из карты testNames
                        val testNameString = testNames[testStatistic.test_id] ?: "Неизвестный тест"
                        testName.text = "Тема ${testStatistic.test_id}. $testNameString"

                        // Количество попыток
                        val attempts = allTestStatistics.count { it.test_id == testStatistic.test_id }
                        attemptsCount.text = "Попыток: $attempts"

                        // Лучший результат
                        val bestScoreValue = allTestStatistics
                            .filter { it.test_id == testStatistic.test_id }
                            .maxByOrNull { it.score }?.score ?: 0

                        // Количество вопросов для теста
                        val questionCount = testQuestionCounts[testStatistic.test_id] ?: 0

                        // Оценка в процентах
                        val percentageScore = if (questionCount > 0) {
                            (bestScoreValue.toDouble() / questionCount * 100).toInt()
                        } else {
                            0
                        }

                        bestScore.text = "Лучший результат: $percentageScore%"

                        // Установка слушателя для "Статистика"
                        statisticsLink.setOnClickListener {
                            onStatisticsClickListener.onStatisticsClicked(testStatistic)
                        }
                    }
                }

                interface OnStatisticsClickListener {
                    fun onStatisticsClicked(testStatistic: TestStatistic)
                }
            }