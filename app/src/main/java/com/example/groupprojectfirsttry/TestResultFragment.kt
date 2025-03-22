package com.example.groupprojectfirsttry

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class TestResultFragment : Fragment(R.layout.fragment_test_result) {

    private lateinit var resultsAdapter: ResultAdapter
    private lateinit var results: List<ResultItem>
    private var score = 0
    private var totalQuestions = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Получаем результаты из аргументов
        val parcelableList = requireArguments().getParcelableArrayList<ResultItem>("results")
        if (parcelableList == null) {
            throw IllegalArgumentException("Results not found")
        }
        results = parcelableList
        score = requireArguments().getInt("score")
        totalQuestions = requireArguments().getInt("totalQuestions")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настройка RecyclerView
        val resultsList = view.findViewById<RecyclerView>(R.id.resultsList)
        resultsList.layoutManager = LinearLayoutManager(context)

        // Настройка адаптера
        resultsAdapter = ResultAdapter(results)
        val itemDecorator = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divider_item!!)
            ?.let { itemDecorator.setDrawable(it) }
        resultsList.addItemDecoration(itemDecorator)
        resultsList.adapter = resultsAdapter

        // Настройка текста оценки
        val tvScore = view.findViewById<TextView>(R.id.tvScore)
        tvScore.text = "Вы набрали $score из $totalQuestions баллов!"

        // Настройка кнопки "Вернуться к тестам"
        val btnBackToTests = view.findViewById<Button>(R.id.btnBackToTests)
        btnBackToTests.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
}