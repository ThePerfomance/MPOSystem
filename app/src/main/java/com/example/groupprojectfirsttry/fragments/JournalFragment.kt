package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.adapters.GroupAdapter
import com.example.groupprojectfirsttry.simpleClasses.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JournalFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_journal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewGroups)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = GroupAdapter(emptyList()) { group ->
            Log.d("JournalFragment", "Group clicked: Name=${group.name}, ID=${group.id}")
            if (group.id == null) {
                Log.e("JournalFragment", "Group ID is null for group: ${group.name}")
                return@GroupAdapter
            }
            val bundle = Bundle().apply {
                putSerializable("groupId", group.id)
            }
            val fragment = StudentListFragment().apply {
                arguments = bundle
            }
            (requireActivity() as SecondActivityWithBottomNavMenu).replaceFragment(fragment,bundle)

        }
        recyclerView.adapter = adapter

        loadGroups()
    }

    private fun loadGroups() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val groups = (activity as SecondActivityWithBottomNavMenu?)!!.getUserGroups()
                if (groups != null) {
                    withContext(Dispatchers.Main) {
                        adapter.groups = groups
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        // Обработка случая, когда группы не найдены
                        Toast.makeText(
                            requireContext(),
                            "Не удалось загрузить группы",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка при загрузке групп: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}