package com.example.groupprojectfirsttry.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.simpleClasses.ResultItem
import com.example.groupprojectfirsttry.adapters.ResultAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView


class TestResultFragment : Fragment(R.layout.fragment_test_result) {

    private lateinit var resultsAdapter: ResultAdapter
    private lateinit var results: List<ResultItem>
    private var score = 0
    private var totalQuestions = 0
    private lateinit var tvUpperLeftCorner: TextView
    private lateinit var tvUpperCenter: TextView
    private lateinit var ivTestLogo: ImageView
    private lateinit var clUpHead: ConstraintLayout
    private lateinit var bnmDown: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //UI

        tvUpperLeftCorner = requireActivity().findViewById(R.id.textViewLeftUpperCorner)
        tvUpperCenter = requireActivity().findViewById(R.id.textViewUpper)
        ivTestLogo = requireActivity().findViewById(R.id.imageViewTestLogo)
        clUpHead = requireActivity().findViewById(R.id.constraintLayoutUpHead)
        bnmDown = requireActivity().findViewById(R.id.bottom_nav)

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)

        tvUpperCenter.text=requireArguments().getString("testTitle")

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
    override fun onPause() {
        super.onPause()
        tvUpperCenter.text=""
        tvUpperCenter.visibility=View.VISIBLE
        tvUpperLeftCorner.visibility=View.GONE

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
    }

    override fun onResume() {
        super.onResume()
        tvUpperCenter.text=requireArguments().getString("testTitle")
        tvUpperCenter.visibility=View.VISIBLE
        tvUpperLeftCorner.visibility=View.GONE

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_gray_background, context?.theme)

    }
    override fun onDestroy() {
        super.onDestroy()
        tvUpperCenter.text=""
        tvUpperCenter.visibility=View.VISIBLE
        tvUpperLeftCorner.visibility=View.GONE

        clUpHead.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
        bnmDown.background = ResourcesCompat.getDrawable(resources,
            R.drawable.gradient_background, context?.theme)
    }
}