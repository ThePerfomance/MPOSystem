package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.adapters.StudentAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import kotlinx.coroutines.launch
import java.util.UUID

class StudentListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentAdapter
    private lateinit var tvGroupHeader: TextView
    private var groupName: String? = null // Название группы

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Загружаем разметку фрагмента
        val view = inflater.inflate(R.layout.fragment_student_list, container, false)

        // Инициализируем RecyclerView после загрузки разметки
        recyclerView = view.findViewById(R.id.recyclerViewStudents)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // UI
        tvGroupHeader = view.findViewById(R.id.textViewStudentsGroupHeader)

        // Получаем данные из аргументов
        val groupId = requireArguments().getSerializable("groupId") as UUID
        groupName = requireArguments().getString("groupName") // Извлекаем название группы
        Log.d("StudentListFragment", "Group Name: $groupName")
        tvGroupHeader.text = groupName

        if (groupId != null) {
            loadStudentsAndTestResults(groupId)
        }

        return view
    }

    private fun loadStudentsAndTestResults(groupId: UUID) = lifecycleScope.launch {
        try {
            // Загружаем студентов группы
            val students = ApiClient.apiService.getGroupUsers(groupId)
            Log.d("StudentListFragment", "Received ${students.size} students for group ID: $groupId")

            // Фильтруем студентов, исключая пользователей с ролью teacher
            val filteredStudents = students.filter { it.role != "teacher" }
            Log.d("StudentListFragment", "Filtered students count: ${filteredStudents.size}")

            // Загружаем результаты теста с id=12 для всех студентов
            val testId = 12
            val testResults = mutableMapOf<UUID, Int>()
            for (student in filteredStudents) {
                val results = student.id?.let { ApiClient.apiService.getUserTestResults(it) }
                val testResult = results?.find { it.test_id == testId }
                if (testResult != null) {
                    testResults[student.id] = testResult.score
                }
            }

            // Создаем адаптер с данными студентов и их результатов
            adapter = StudentAdapter(filteredStudents, testResults)
            recyclerView.adapter = adapter

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("StudentListFragment", "Error fetching students or test results: ${e.message}")
        }
    }
}