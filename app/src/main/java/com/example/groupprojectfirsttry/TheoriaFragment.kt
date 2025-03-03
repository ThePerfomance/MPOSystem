package com.example.groupprojectfirsttry

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.LeadingMarginSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream

class TheoriaFragment : Fragment(R.layout.fragment_theoria) {

    private var ivBooks: ImageView? = null
    private var ivThreeLinesMenu: ImageView? = null
    private var chapterSpinner: Spinner? = null
    private lateinit var adapter: TheoriaAdapter
    private lateinit var recyclerView: RecyclerView
    private val chapters = arrayOf(
        "Введение",
        "1. Основы языка разметки HTML",
        "2. Работа с формами в HTML5",
        "3. Семантическая верстка страниц в HTML5",
        "4. Работа с каскадными таблицами стилей",
        "5. Фильтры в CSS",
        "6. Блоковые элементы в CSS",
        "7. Трансформации, переходы и анимации",
        "8. Адаптивная верстка",
        "9. Создание гибкого макета страницы с помощью Flexbox",
        "10. Двумерная система сеток Grid Layout",
        "11. Использование переменных в CSS",
        "Заключение"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_theoria, container, false)

        // Настройка UI элементов
        ivBooks = requireActivity().findViewById(R.id.imageViewBooks)
        ivThreeLinesMenu = requireActivity().findViewById(R.id.imageViewThreeLinesMenu)
        chapterSpinner = requireActivity().findViewById(R.id.chapterSpinner)

        setupRecyclerView(view)
        setupSpinner()
        setupMenuButton()

        return view
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = TheoriaAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupSpinner() {
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_selected_item,
            chapters
        )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)

        chapterSpinner?.apply {
            adapter = spinnerAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    loadChapter(chapters[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private fun setupMenuButton() {
        ivThreeLinesMenu?.setOnClickListener {
            chapterSpinner?.performClick()
        }
    }

    private fun loadChapter(chapterTitle: String) {
        val fileName = getFileNameByChapter(chapterTitle)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileContent = readFileFromAssets(fileName)
                withContext(Dispatchers.Main) {
                    adapter.setItems(fileContent)
                    // Сброс позиции RecyclerView в начало
                    recyclerView.scrollToPosition(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка загрузки файла", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileNameByChapter(chapterTitle: String): String = when (chapterTitle) {
        "Введение" -> "0Vvedenie.docx"
        "1. Основы языка разметки HTML" -> "1VvedenieHTML.docx"
        "2. Работа с формами в HTML5" -> "2RabotaSFormami.docx"
        "3. Семантическая верстка страниц в HTML5" -> "3VerstkaStranits.docx"
        "4. Работа с каскадными таблицами стилей" -> "4CSSCascadeTables.docx"
        "5. Фильтры в CSS" -> "5CSSFilters.docx"
        "6. Блоковые элементы в CSS" -> "6CSSBlockElements.docx"
        "7. Трансформации, переходы и анимации" -> "7TransformationAndAnimation.docx"
        "8. Адаптивная верстка" -> "8AdaptiveVerstka.docx"
        "9. Создание гибкого макета страницы с помощью Flexbox" -> "9FlexibleMaket.docx"
        "10. Двумерная система сеток Grid Layout" -> "10GridLayout.docx"
        "11. Использование переменных в CSS" -> "11UsingPeremenInCSS.docx"
        "Заключение" -> "99FinalWords.docx"
        else -> "0Vvedenie.docx" // Файл по умолчанию
    }

    private suspend fun readFileFromAssets(fileName: String): List<Any> {
        val items = mutableListOf<Any>()
        requireContext().assets.open(fileName).use { inputStream ->
            XWPFDocument(inputStream).use { document ->
                for (paragraph in document.paragraphs) {
                    val spannableString = processParagraph(paragraph)
                    if (spannableString.isNotEmpty()) {
                        items.add(addParagraphIndent(spannableString))
                    }
                    processImages(paragraph, items)
                }
            }
        }
        return items
    }

    private fun processParagraph(paragraph: org.apache.poi.xwpf.usermodel.XWPFParagraph): SpannableStringBuilder {
        val spannableString = SpannableStringBuilder()
        for (run in paragraph.runs) {
            val text = run.text()
            if (!text.isNullOrEmpty()) {
                val start = spannableString.length
                spannableString.append(text)
                val end = spannableString.length

                if (run.isBold) {
                    spannableString.setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        start,
                        end,
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                if (run.isItalic) {
                    spannableString.setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.ITALIC),
                        start,
                        end,
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        return spannableString
    }

    private fun processImages(paragraph: org.apache.poi.xwpf.usermodel.XWPFParagraph, items: MutableList<Any>) {
        for (run in paragraph.runs) {
            for (picture in run.embeddedPictures) {
                val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(picture.pictureData.data))
                items.add(bitmap)
            }
        }
    }

    private fun addParagraphIndent(text: SpannableStringBuilder): SpannableStringBuilder {
        text.setSpan(
            LeadingMarginSpan.Standard(40), // Размер отступа
            0,
            text.length,
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return text
    }

    override fun onPause() {
        super.onPause()
        ivBooks?.visibility = View.INVISIBLE
        ivThreeLinesMenu?.visibility = View.INVISIBLE
        chapterSpinner?.visibility = View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
        ivBooks?.visibility = View.VISIBLE
        ivThreeLinesMenu?.visibility = View.VISIBLE
        chapterSpinner?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ivBooks = null
        ivThreeLinesMenu = null
        chapterSpinner = null
    }
}