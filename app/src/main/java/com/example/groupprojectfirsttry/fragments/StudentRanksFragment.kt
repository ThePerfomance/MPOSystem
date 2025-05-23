package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.adapters.StudentRankAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.simpleClasses.User
import com.example.groupprojectfirsttry.MathMethods.KMeans
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.simpleClasses.StudentData
import kotlinx.coroutines.launch
import java.util.*

class StudentRanksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentRankAdapter
    private lateinit var textViewSilhouetteScore: TextView
    private lateinit var textViewInertia: TextView
    private lateinit var textViewDBI: TextView
    private lateinit var btnShowClusterChart: Button

    private var groupId: UUID? = null
    private var groupName: String? = null
    private var lastPoints: List<KMeans.Point>? = null

    companion object {
        fun newInstance(groupId: UUID, groupName: String): StudentRanksFragment {
            val fragment = StudentRanksFragment()
            val args = Bundle().apply {
                putSerializable("groupId", groupId)
                putString("groupName", groupName)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getSerializable("groupId") as UUID
            groupName = it.getString("groupName")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_student_ranks, container, false)

        // UI
        recyclerView = view.findViewById(R.id.recyclerViewStudentRanks)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        textViewSilhouetteScore = view.findViewById(R.id.textViewSilhouetteScore)
        textViewInertia = view.findViewById(R.id.textViewInertia)
        textViewDBI = view.findViewById(R.id.textViewDBI)
        btnShowClusterChart = view.findViewById(R.id.buttonShowClusterChart)

        // Загружаем данные
        groupId?.let { loadStudentRanks(it) }

        // Переход к графику кластеризации
        btnShowClusterChart.setOnClickListener {
            lastPoints?.let { points ->
                val fragment = ClusterChartFragment.newInstance(points)
                (requireActivity() as SecondActivityWithBottomNavMenu).replaceFragment(fragment)
            } ?: run {
                Log.e("StudentRanksFragment", "Нет данных для построения графика")
            }
        }
        val buttonShowDetails: Button = view.findViewById(R.id.buttonShowDetails)
        buttonShowDetails.setOnClickListener {
            showAlgorithmInfoDialog()
        }

        return view
    }
    private fun loadStudentRanks(groupId: UUID) = lifecycleScope.launch {
        try {
            // 1. Получаем всех пользователей группы
            val groupUsers = ApiClient.apiService.getGroupUsers(groupId)
            val students = groupUsers.filter { it.role == "student" }

            // 2. Собираем данные для каждого студента
            val studentDataList = mutableListOf<Pair<User, Pair<StudentData, String>>>()
            val allStudentData = mutableListOf<StudentData>()

            for (user in students) {
                val data = user.getStudentData()
                allStudentData.add(data)
                studentDataList.add(Pair(user, Pair(data, "")))
            }

            // 3. Кластеризуем студентов
            val (rankedStudents, points) = KMeans.classifyStudents(allStudentData)
            lastPoints = points

            // 4. Добавляем ранг каждому студенту
            studentDataList.replaceAll { pair ->
                val rank = rankedStudents[pair.second.first] ?: "Не определено"
                Pair(pair.first, Pair(pair.second.first, rank))
            }

            // 5. Сортируем по фамилии и имени
            val sortedStudentList = studentDataList.sortedWith(
                compareBy({ it.first.lastname }, { it.first.firstname })
            )

            // 6. Передаём данные в адаптер
            adapter = StudentRankAdapter(sortedStudentList)
            recyclerView.adapter = adapter

            // Вычисляем метрики
            val silhouetteScore = KMeans.calculateSilhouetteScore(points)
            val inertia = KMeans.calculateInertia(points)
            val daviessBouldin = KMeans.daviesBouldinIndex(points)

            // Обновляем UI
            textViewSilhouetteScore.text = "Коэффициент силуэта: ${String.format("%.3f", silhouetteScore)}"
            textViewInertia.text = "Инерция (сумма квадратов): ${String.format("%.3f", inertia)}"
            textViewDBI.text = "Индекс Дэвиса–Боулдина: ${String.format("%.3f", daviessBouldin)}"

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("StudentRanksFragment", "Ошибка при загрузке данных: ${e.message}")
        }
    }
    private fun showAlgorithmInfoDialog() {
        val message = """
        Алгоритм кластеризации: KMeans + PCA
        
        1. KMeans:
           - Метод кластеризации, который группирует студентов по признакам.
           - Используются: точность, попытки, время, количество тестов, сложность.
           - Перед запуском все данные нормализуются для равного влияния признаков.
        
        2. PCA (Principal Component Analysis):
           - Снижает размерность данных до 2D/3D для упрощения визуализации.
           - Позволяет лучше разделить кластеры и убрать шумы.
        
        3. Ранжирование:
           - После кластеризации студенты получают ранг S, A, B, C или D.
           - Ранги назначаются на основе коэффициента силуэта и средней точности кластера.
        
        4. Silhouette Score:
           - Отражает, насколько хорошо точки разделены между кластерами.
           - Значение от -1 до 1:
               • Близко к 1 → отличная кластеризация
               • Около 0 → пересечение кластеров
               • Близко к -1 → неправильное присвоение кластеров
        
        5. Inertia (Sum of Squared Distances to Centroids):
           - Сумма квадратов расстояний всех точек до центроида своего кластера.
           - Чем меньше значение — тем плотнее кластеры.
        
        6. Davies-Bouldin Index:
           - Измеряет среднюю похожесть между каждым кластером и его ближайшим соседом.
           - Значение близко к 0 — идеально разделённые кластеры.
           - Значение выше 1 — плохое разделение кластеров.
           - Используется для сравнения качества кластеризации между разными параметрами.

        📊 Интерпретация:
            • **Silhouette** > 0.5 — хорошие кластеры
            • **Inertia** → чем ниже, тем лучше
            • **DBI** < 1 — хорошие кластеры
    """.trimIndent()

        val dialog = activity?.let { context ->
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Как работает алгоритм")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .create()
        }

        dialog?.show()
    }
}