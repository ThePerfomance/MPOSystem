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
            // Запрос на сервер — вся кластеризация происходит там
            val response = ApiClient.apiService.clusterGroup(groupId)

            if (!response.isSuccessful) {
                Log.e("StudentRanksFragment", "Ошибка: ${response.code()}")
                return@launch
            }

            val data = response.body() ?: return@launch

            // Получаем пользователей группы для отображения имён
            val groupUsers = ApiClient.apiService.getGroupUsers(groupId)
            val userMap = groupUsers.associateBy { it.id.toString() }

            // Формируем список для адаптера
            val studentDataList = data.clusters.mapNotNull { cluster ->
                val user = userMap[cluster.user_id] ?: return@mapNotNull null
                val studentData = StudentData(
                    accuracy            = cluster.avg_score.toDouble(),
                    attempts            = cluster.tests_taken.toDouble(),
                    timeSpent           = 0.0,
                    testCount           = cluster.tests_taken.toDouble(),
                    weightedDifficulty  = 0.0
                )
                Pair(user, Pair(studentData, cluster.rank))
            }

            // Сохраняем PCA-точки для графика
            lastPoints = data.pca_points.map { p ->
                KMeans.Point(listOf(p.x.toDouble(), p.y.toDouble()), p.cluster_id)
            }

            // Сортируем по фамилии
            val sorted = studentDataList.sortedWith(
                compareBy({ it.first.lastname }, { it.first.firstname })
            )

            adapter = StudentRankAdapter(sorted)
            recyclerView.adapter = adapter

            // Метрики
            textViewSilhouetteScore.text = "Коэффициент силуэта: ${
                String.format("%.3f", data.metrics.silhouette)}"
            textViewInertia.text = "Инерция: ${
                String.format("%.3f", data.metrics.inertia)}"
            textViewDBI.text = "Метрики рассчитаны на сервере"

        } catch (e: Exception) {
            Log.e("StudentRanksFragment", "Ошибка: ${e.message}")
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