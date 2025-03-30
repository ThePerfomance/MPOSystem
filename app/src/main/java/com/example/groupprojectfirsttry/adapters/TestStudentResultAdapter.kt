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
                private val allTestStatistics: List<TestStatistic>, // Полный список для расчетов
                private val testQuestionCounts: Map<Int, Int> // Количество вопросов для каждого теста
            ) : RecyclerView.Adapter<TestStudentResultAdapter.TestResultViewHolder>() {

                // Фильтруем список, чтобы оставить только уникальные test_id
                private val uniqueTestStatistics = testStatistics.distinctBy { it.test_id }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestResultViewHolder {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.test_student_result_item, parent, false)
                    return TestResultViewHolder(view, allTestStatistics, testQuestionCounts)
                }

                override fun onBindViewHolder(holder: TestResultViewHolder, position: Int) {
                    val testStatistic = uniqueTestStatistics[position]
                    holder.bind(testStatistic)
                }

                override fun getItemCount(): Int = uniqueTestStatistics.size

                class TestResultViewHolder(
                    itemView: View,
                    private val allTestStatistics: List<TestStatistic>,
                    private val testQuestionCounts: Map<Int, Int>
                ) : RecyclerView.ViewHolder(itemView) {

                    private val testName: TextView = itemView.findViewById(R.id.testName)
                    private val attemptsCount: TextView = itemView.findViewById(R.id.attemptsCount)
                    private val bestScore: TextView = itemView.findViewById(R.id.bestScore)

                    fun bind(testStatistic: TestStatistic) {
                        testName.text = "Тест ${testStatistic.test_id}"

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
                    }
                }
            }