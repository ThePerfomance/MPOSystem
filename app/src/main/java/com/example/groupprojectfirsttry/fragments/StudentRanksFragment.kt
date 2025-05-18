package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.adapters.StudentRankAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.simpleClasses.User
import com.example.groupprojectfirsttry.KMeans
import com.example.groupprojectfirsttry.simpleClasses.StudentData
import kotlinx.coroutines.launch
import java.util.*
import com.github.mikephil.charting.data.Entry

class StudentRanksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentRankAdapter
    private lateinit var textViewSilhouetteScore: TextView

    private var groupId: UUID? = null
    private var groupName: String? = null

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

        // Инициализируем элементы интерфейса
        recyclerView = view.findViewById(R.id.recyclerViewStudentRanks)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        textViewSilhouetteScore = view.findViewById(R.id.textViewSilhouetteScore)

        // Загружаем данные
        groupId?.let { loadStudentRanks(it) }

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

            // 4. Добавляем ранг каждому студенту
            studentDataList.replaceAll { pair ->
                val rank = rankedStudents[pair.second.first] ?: "Не определено"
                Pair(pair.first, Pair(pair.second.first, rank))
            }

            // 5. Передаём данные в адаптер
            val sortedStudentList = studentDataList.sortedWith(
                compareBy({ it.first.lastname }, { it.first.firstname })
            )

            adapter = StudentRankAdapter(sortedStudentList)
            recyclerView.adapter = adapter

            // 6. Рассчитываем Silhouette Score и выводим его числом
            val silhouetteScore = calculateSilhouetteScore(points)
            textViewSilhouetteScore.text =
                "Коэффициент Силуэта: ${String.format("%.2f", silhouetteScore)}"

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("StudentRanksFragment", "Ошибка при загрузке данных: ${e.message}")
        }
    }

    private fun calculateSilhouetteScore(points: List<KMeans.Point>): Double {
        return KMeans.calculateSilhouetteScore(points).coerceIn(-1.0..1.0)
    }
}