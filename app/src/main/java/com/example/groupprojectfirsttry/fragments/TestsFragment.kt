package com.example.groupprojectfirsttry.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.simpleClasses.Test
import com.example.groupprojectfirsttry.simpleClasses.User
import com.example.groupprojectfirsttry.adapters.TestsAdapter
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TokenManager
import com.example.groupprojectfirsttry.simpleClasses.Lesson
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class TestsFragment : Fragment(R.layout.fragment_tests) {

    private lateinit var clUpHead: ConstraintLayout
    private lateinit var bnmDown: BottomNavigationView
    private lateinit var testList: RecyclerView
    private lateinit var adapter: TestsAdapter
    private lateinit var tvUpperLeftCorner: TextView
    private lateinit var tvUpperCenter: TextView
    private lateinit var ivTestLogo: ImageView
    private lateinit var user: User
    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tests, container, false)
        tokenManager = ApiClient.getTokenManager() ?: TokenManager(requireContext())
        user = (activity as? SecondActivityWithBottomNavMenu)?.getUser() ?: return view
        
        clUpHead = requireActivity().findViewById(R.id.constraintLayoutUpHead)
        bnmDown = requireActivity().findViewById(R.id.bottom_nav)

        tvUpperLeftCorner = requireActivity().findViewById(R.id.textViewLeftUpperCorner)
        tvUpperCenter = requireActivity().findViewById(R.id.textViewUpper)
        ivTestLogo = requireActivity().findViewById(R.id.imageViewTestLogo)

        testList = view.findViewById(R.id.testListContainer)
        testList.layoutManager = LinearLayoutManager(context)

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)

        tvUpperCenter.visibility = View.GONE
        tvUpperLeftCorner.visibility = View.VISIBLE
        tvUpperLeftCorner.text = "Оценка знаний"
        ivTestLogo.visibility = View.VISIBLE

        adapter = TestsAdapter(
            emptyList(),
            onArrowClick = { test -> startTest(test) },
        )
        val itemDecorator = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divider_item)?.let {
            itemDecorator.setDrawable(it)
        }
        testList.addItemDecoration(itemDecorator)
        testList.adapter = adapter

        loadTests()

        return view
    }

    private fun loadTests() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val subjectId = tokenManager.getSelectedSubjectId()
                if (subjectId == null) {
                    Toast.makeText(context, "Выберите предмет на главном экране", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val blocks = ApiClient.apiService.getBlocksBySubject(subjectId)
                val allTestsForSubject = mutableListOf<Test>()
                
                blocks.map { block ->
                    async {
                        try {
                            // 1. Тесты из уроков
                            val lessons = ApiClient.apiService.getLessonsByBlock(block.id)
                            lessons.forEach { lesson ->
                                if (lesson.test != null) {
                                    try {
                                        val test = ApiClient.apiService.getTestForLesson(lesson.id)
                                        synchronized(allTestsForSubject) { allTestsForSubject.add(test) }
                                    } catch (e: Exception) { }
                                }
                            }
                            // 2. Финальный тест блока
                            if (block.finalTestId != null) {
                                try {
                                    val finalTest = ApiClient.apiService.getFinalTestForBlock(block.id)
                                    synchronized(allTestsForSubject) { allTestsForSubject.add(finalTest) }
                                } catch (e: Exception) { }
                            }
                        } catch (e: Exception) {
                            Log.e("TestsFragment", "Error loading tests for block ${block.id}", e)
                        }
                        Unit // Явно возвращаем Unit, чтобы if не считался выражением
                    }
                }.awaitAll()

                val testResults = user.id?.let { ApiClient.apiService.getUserTestResults(it) } ?: emptyList()
                val groupedTestResults = testResults.groupBy { it.test_id }

                adapter.updateTests(allTestsForSubject.distinctBy { it.id }, groupedTestResults)
                
            } catch (e: Exception) {
                Log.e("TestsFragment", "Error in loadTests", e)
                Toast.makeText(context, "Ошибка загрузки тестов", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTest(test: Test) {
        val args = Bundle().apply {
            putParcelable("test", test)
            putParcelable("user", user)
        }
        (requireActivity() as? SecondActivityWithBottomNavMenu)?.replaceFragment(TestPassFragment(), args)
    }

    override fun onPause() {
        super.onPause()
        tvUpperCenter.visibility = View.VISIBLE
        tvUpperLeftCorner.visibility = View.GONE
        ivTestLogo.visibility = View.GONE

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
    }

    override fun onResume() {
        super.onResume()
        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)

        tvUpperCenter.visibility = View.GONE
        tvUpperLeftCorner.visibility = View.VISIBLE
        tvUpperLeftCorner.text = "Оценка знаний"
        ivTestLogo.visibility = View.VISIBLE
    }
}
