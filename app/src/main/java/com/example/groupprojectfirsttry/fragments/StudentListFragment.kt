package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

        // Получаем ID группы из аргументов
        val groupId=requireArguments().getSerializable("groupId") as UUID
        if (groupId != null) {
            loadStudents(groupId)
        }

        return view
    }

    private fun loadStudents(groupId: UUID) = lifecycleScope.launch {
        try {
            val students = ApiClient.apiService.getGroupUsers(groupId)
            Log.d("StudentListFragment", "Received ${students.size} students for group ID: $groupId")
            adapter = StudentAdapter(students)
            recyclerView.adapter = adapter
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("StudentListFragment", "Error fetching students: ${e.message}")
        }
    }
}