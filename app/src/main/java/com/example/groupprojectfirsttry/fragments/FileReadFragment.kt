package com.example.groupprojectfirsttry.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.LeadingMarginSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.adapters.TheoriaAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream
import java.io.InputStream

class FileReadFragment(private val fileName: String) : Fragment() {

    private lateinit var adapter: TheoriaAdapter
    private lateinit var recyclerView: RecyclerView
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

        // Загружаем файл после инициализации RecyclerView
        loadChapter(fileName)

        return view
    }
    private fun loadChapter(chapterTitle: String) {
        // Определяем имя файла на основе названия главы
        val fileName = when (chapterTitle) {
            "Лабораторная работа № 1. Создание статических Web-страниц с использованием HTML" -> "Lab_rab_1.docx"
            "Лабораторная работа № 2. Разработка стилизованных веб-сайтов средствами каскадных таблиц стилей (CSS)" -> "Lab_rab_2.docx"
            "Лабораторная работа № 3. Создание Web-сайтов с включением сценариев на языке JavaScript" -> "Lab_rab_3.docx"
            "Лабораторная работа № 4. Программирование Web-сайтов на стороне Web-сервера Apache средствами языка PHP" -> "Lab_rab_4.docx"
            "Лабораторная работа № 5. Создание базы данных в СУБД MySQL" -> "Lab_rab_5.docx"
            else -> "0Vvedenie.docx" // Файл по умолчанию
        }
        // Загружаем содержимое файла
        loadFile(fileName)
    }

    private fun loadFile(fileName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream: InputStream = requireContext().assets.open(fileName)
                val document = XWPFDocument(inputStream)
                val newItems = mutableListOf<Any>()

                // Сбрасываем счетчик перед началом обработки нового файла
                listCounter = 1

                for (paragraph in document.paragraphs) {
                    val spannableString = SpannableStringBuilder()

                    // Проверяем, является ли параграф частью списка
                    val isList = paragraph.numIlvl != null || paragraph.numFmt != null

                    for (run in paragraph.runs) {
                        val text = run.text()
                        if (!text.isNullOrEmpty()) {
                            val start = spannableString.length
                            spannableString.append(text)
                            val end = spannableString.length

                            // Применяем форматирование (жирный, курсив)
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

                    // Если параграф является частью списка, добавляем нумерацию
                    if (isList && spannableString.isNotEmpty()) {
                        spannableString.insert(0, "$listCounter. ") // Добавляем нумерацию
                        listCounter++ // Увеличиваем счетчик
                    } else {
                        // Если параграф не является частью списка, сбрасываем счетчик
                        listCounter = 1
                    }

                    if (spannableString.isNotEmpty()) {
                        newItems.add(addParagraphIndent(spannableString))
                    }

                    // Обработка изображений
                    for (run in paragraph.runs) {
                        val pictures = run.embeddedPictures
                        for (picture in pictures) {
                            val bitmap = BitmapFactory.decodeStream(
                                ByteArrayInputStream(picture.pictureData.data)
                            )
                            newItems.add(bitmap)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    adapter.setItems(newItems)
                }

                document.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка загрузки файла", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addParagraphIndent(text: SpannableStringBuilder): SpannableStringBuilder {
        // Добавляем отступ в начале абзаца (например, 40 пикселей)
        text.setSpan(
            LeadingMarginSpan.Standard(40), // Размер отступа
            0,
            text.length,
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return text
    }
}