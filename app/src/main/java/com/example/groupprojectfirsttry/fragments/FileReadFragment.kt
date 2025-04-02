package com.example.groupprojectfirsttry.fragments

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AlignmentSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.adapters.TheoriaAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.*
import java.io.ByteArrayInputStream
import java.io.InputStream

class FileReadFragment(private val fileName: String, private val color: String) : Fragment() {
    private lateinit var adapter: TheoriaAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvUpperLeftCorner: TextView
    private lateinit var tvUpperCenter: TextView
    private lateinit var ivLabWorkLogo: ImageView
    private lateinit var clUpHead: ConstraintLayout
    private lateinit var bnmDown: BottomNavigationView
    private var listCounter = 1 // Счетчик для нумерации

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_theoria, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = TheoriaAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // UI
        tvUpperLeftCorner = requireActivity().findViewById(R.id.textViewLeftUpperCorner)
        tvUpperCenter = requireActivity().findViewById(R.id.textViewUpper)
        ivLabWorkLogo = requireActivity().findViewById(R.id.imageViewLabWorkLogo)
        clUpHead = requireActivity().findViewById(R.id.constraintLayoutUpHead)
        bnmDown = requireActivity().findViewById(R.id.bottom_nav)

        when (color) {
            "blue" -> {
                clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
                bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
                tvUpperCenter.text = "Практические работы"
            }
            "gray" -> {
                clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_gray_background, context?.theme)
                bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_gray_background, context?.theme)
                tvUpperCenter.text = "Лабораторные работы"
            }
            else -> {
                clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
                bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
            }
        }

        tvUpperCenter.visibility = View.VISIBLE
        tvUpperLeftCorner.visibility = View.GONE
        ivLabWorkLogo.visibility = View.GONE

        // Загружаем файл после инициализации RecyclerView
        loadFile(fileName)
        return view
    }

    private fun loadFile(fileName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().assets.open(fileName)
                val document = XWPFDocument(inputStream)
                val newItems = mutableListOf<Any>()
                var currentListNumber = 1
                var lastListLevel = -1

                for (paragraph in document.paragraphs) {
                    val spannable = SpannableStringBuilder()
                    // Безопасная проверка стиля параграфа
                    val rawParagraphStyle = paragraph.style?.toLowerCase() ?: ""
                    val isHeading = rawParagraphStyle.contains("heading") ||
                            rawParagraphStyle.contains("заголовок") ||
                            rawParagraphStyle.contains("глава") ||
                            rawParagraphStyle == "title"

                    val listLevel = getParagraphListLevel(paragraph)
                    val isListItem = listLevel != -1

                    // Обработка текста и стилей
                    paragraph.runs.forEach { run ->
                        val text = run.text() ?: ""
                        val start = spannable.length
                        spannable.append(text)
                        if (run.isBold) {
                            spannable.setSpan(
                                StyleSpan(Typeface.BOLD),
                                start, spannable.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        if (run.isItalic) {
                            spannable.setSpan(
                                StyleSpan(Typeface.ITALIC),
                                start, spannable.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }

                    if (spannable.isNotEmpty()) {
                        if (isHeading) {
                            // Для заголовков
                            spannable.setSpan(
                                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                                0, spannable.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            spannable.setSpan(
                                LeadingMarginSpan.Standard(0, 0),
                                0, spannable.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        } else if (isListItem) {
                            // Для пунктов списка
                            if (listLevel > lastListLevel) {
                                currentListNumber = 1
                            } else if (listLevel < lastListLevel) {
                                currentListNumber = 1
                            }
                            val listItemText = "$currentListNumber. $spannable"
                            val listItemSpannable = SpannableStringBuilder(listItemText)
                            listItemSpannable.setSpan(
                                LeadingMarginSpan.Standard(
                                    (40 * resources.displayMetrics.density).toInt() * (listLevel + 1),
                                    0
                                ),
                                0, listItemSpannable.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            newItems.add(listItemSpannable)
                            currentListNumber++
                        } else {
                            // Для обычного текста
                            spannable.setSpan(
                                LeadingMarginSpan.Standard(
                                    (40 * resources.displayMetrics.density).toInt(),
                                    0
                                ),
                                0, spannable.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            newItems.add(spannable)
                        }
                    }

                    // Обработка изображений
                    paragraph.runs.flatMap { it.embeddedPictures }.forEach { picture ->
                        BitmapFactory.decodeStream(ByteArrayInputStream(picture.pictureData.data))?.let {
                            newItems.add(it)
                        }
                    }

                    lastListLevel = listLevel
                }

                withContext(Dispatchers.Main) {
                    adapter.setItems(newItems)
                }
                document.close()
                inputStream.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun getParagraphListLevel(paragraph: XWPFParagraph): Int {
        val ilvl = paragraph.numIlvl
        return ilvl?.toInt() ?: -1
    }
    override fun onPause() {
        super.onPause()
        tvUpperCenter.visibility = View.VISIBLE
        tvUpperLeftCorner.visibility = View.GONE
        ivLabWorkLogo.visibility = View.GONE
        tvUpperCenter.text = ""
        updateBackgrounds("gray")
    }

    override fun onResume() {
        super.onResume()
        tvUpperCenter.visibility = View.VISIBLE
        tvUpperLeftCorner.visibility = View.GONE
        ivLabWorkLogo.visibility = View.GONE
        updateBackgrounds(color)
    }

    override fun onDestroy() {
        super.onDestroy()
        tvUpperCenter.visibility = View.VISIBLE
        tvUpperLeftCorner.visibility = View.GONE
        ivLabWorkLogo.visibility = View.GONE
        tvUpperCenter.text = ""
        updateBackgrounds("gray")
    }

    private fun updateBackgrounds(color: String) {
        when (color) {
            "blue" -> {
                clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
                bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
                tvUpperCenter.text = "Практические работы"
            }
            "gray" -> {
                clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_gray_background, context?.theme)
                bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_gray_background, context?.theme)
                tvUpperCenter.text = "Лабораторные работы"
            }
            else -> {
                clUpHead.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
                bnmDown.background = ResourcesCompat.getDrawable(resources, R.drawable.gradient_background, context?.theme)
            }
        }
    }
}