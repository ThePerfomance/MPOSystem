package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.adapters.StudentAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import kotlinx.coroutines.launch
import java.util.UUID

class StudentListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentAdapter
    private lateinit var tvGroupHeader: TextView
    private lateinit var btnRanks: Button
    private var groupName: String? = null // Название группы
    private var groupId: UUID?=null

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
        btnRanks=view.findViewById(R.id.buttonRanks)
        btnRanks.setOnClickListener { buttonRanksOnClick() }
        // Получаем данные из аргументов
        groupId = requireArguments().getSerializable("groupId") as UUID
        groupName = requireArguments().getString("groupName") // Извлекаем название группы
        Log.d("StudentListFragment", "Group Name: $groupName")
        tvGroupHeader.text = groupName

        groupId?.let { loadStudentsAndTestResults(it) }

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

            // Сортируем студентов по алфавиту: фамилия -> имя -> отчество
            val sortedStudents = filteredStudents.sortedWith(
                compareBy(
                    { it.lastname },  // Сначала сортировка по фамилии
                    { it.firstname }, // Затем по имени
                    { it.patronymic } // И наконец по отчеству
                )
            )
            Log.d("StudentListFragment", "Sorted students count: ${sortedStudents.size}")

            // Загружаем результаты теста с id=12 для всех студентов
            val testId = 12
            val testResults = mutableMapOf<UUID, Int>()
            for (student in sortedStudents) {
                val results = student.id?.let { ApiClient.apiService.getUserTestResults(it) }
                val testResult = results?.find { it.test_id == testId }
                if (testResult != null) {
                    testResults[student.id] = testResult.score
                }
            }

            // Создаем адаптер с данными студентов и их результатов
            adapter = StudentAdapter(sortedStudents, testResults)
            recyclerView.adapter = adapter

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("StudentListFragment", "Error fetching students or test results: ${e.message}")
        }
    }
    private fun buttonRanksOnClick() {
        val groupId = groupId ?: run {
            Log.e("StudentListFragment", "Group ID is null")
            return
        }

        val groupName = groupName ?: run {
            Log.e("StudentListFragment", "Group name is null")
            return
        }

        val args = Bundle().apply {
            putSerializable("groupId", groupId)
            putString("groupName", groupName)
        }

        val fragment = StudentRanksFragment()
        fragment.arguments = args

        (requireActivity() as SecondActivityWithBottomNavMenu).replaceFragment(fragment, args)
    }
}