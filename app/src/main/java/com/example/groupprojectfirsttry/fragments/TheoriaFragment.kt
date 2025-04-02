    package com.example.groupprojectfirsttry.fragments

    import android.graphics.BitmapFactory
    import android.graphics.Typeface
    import android.os.Bundle
    import android.text.Layout
    import android.text.Spannable
    import android.text.SpannableStringBuilder
    import android.text.style.AlignmentSpan
    import android.text.style.LeadingMarginSpan
    import android.text.style.RelativeSizeSpan
    import android.text.style.StyleSpan
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.AdapterView
    import android.widget.ArrayAdapter
    import android.widget.ImageView
    import android.widget.Spinner
    import android.widget.TextView
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
    import java.util.Locale

    class TheoriaFragment : Fragment(R.layout.fragment_theoria) {
        private var ivBooks: ImageView? = null
        private var ivThreeLinesMenu: ImageView? = null
        private var chapterSpinner: Spinner? = null
        private var tvLeftCornerTitle: TextView? = null
        private var tvCenterTitle: TextView? = null
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
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_theoria, container, false)
            ivBooks = requireActivity().findViewById(R.id.imageViewBooks)
            ivThreeLinesMenu=requireActivity().findViewById(R.id.imageViewThreeLinesMenu)
            chapterSpinner = requireActivity().findViewById(R.id.chapterSpinner)
            tvLeftCornerTitle=requireActivity().findViewById(R.id.textViewLeftUpperCorner)
            tvCenterTitle=requireActivity().findViewById(R.id.textViewUpper)

            tvCenterTitle?.visibility = View.GONE
            ivBooks?.visibility = View.VISIBLE
            ivThreeLinesMenu?.visibility = View.VISIBLE
            chapterSpinner?.visibility=View.VISIBLE
            tvLeftCornerTitle?.visibility=View.VISIBLE
            tvLeftCornerTitle?.text="Теоретический\nматериал"
            //
            //  SPINNER
            //
            val adapterSpinner = ArrayAdapter(
                requireContext(),
                R.layout.spinner_selected_item,
                chapters
            )
            adapterSpinner.setDropDownViewResource(R.layout.spinner_item)

            chapterSpinner?.adapter = adapterSpinner
            chapterSpinner?.onItemSelectedListener  = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    // Получаем выбранную главу
                    val selectedChapter = chapters[position]
                    loadChapter(selectedChapter)
                    scrollToTop()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            //
            // recyclerView
            //
            recyclerView = view.findViewById(R.id.recyclerView)
            adapter = TheoriaAdapter()
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            //
            //ivThreeLinesMenu
            //
            ivThreeLinesMenu?.setOnClickListener {

                chapterSpinner?.visibility = View.VISIBLE
                chapterSpinner?.performClick()
            }
            return view
        }
        private fun loadChapter(chapterTitle: String) {
            // Определяем имя файла на основе названия главы
            val fileName = when (chapterTitle) {
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

            // Загружаем содержимое файла
            loadFile(fileName)
        }
        private fun loadFile(fileName: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val inputStream = requireContext().assets.open(fileName)
                    val document = XWPFDocument(inputStream)
                    val newItems = mutableListOf<Any>()

                    for (paragraph in document.paragraphs) {
                        val spannable = SpannableStringBuilder()

                        // Безопасная проверка стиля параграфа
                        val rawParagraphStyle = paragraph.style?.toLowerCase() ?: "" // Добавлена проверка на null
                        val isHeading = rawParagraphStyle.contains("heading") ||
                                rawParagraphStyle.contains("заголовок") ||
                                rawParagraphStyle.contains("глава") ||
                                rawParagraphStyle == "title"

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
                            }
                            newItems.add(spannable)
                        }

                        // Обработка изображений
                        paragraph.runs.flatMap { it.embeddedPictures }.forEach { picture ->
                            BitmapFactory.decodeStream(ByteArrayInputStream(picture.pictureData.data))?.let {
                                newItems.add(it)
                            }
                        }
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
        private fun scrollToTop() {
            recyclerView.scrollToPosition(0) // Замените `binding.recyclerView` на ваш RecyclerView
        }
        private fun addParagraphIndent(text: SpannableStringBuilder): SpannableStringBuilder {
            // Добавляем отступ первой строки (40 пикселей) и отступ остальных строк (0 пикселей)
            text.setSpan(
                LeadingMarginSpan.Standard(
                    (40 * resources.displayMetrics.density).toInt(), // Отступ первой строки
                    (0 * resources.displayMetrics.density).toInt()  // Отступ остальных строк
                ),
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
            chapterSpinner?.visibility=View.INVISIBLE
            tvCenterTitle?.visibility = View.VISIBLE
            tvLeftCornerTitle?.visibility=View.GONE
            tvLeftCornerTitle?.text=""
        }
        override fun onResume() {
            super.onResume()
            tvCenterTitle?.visibility = View.GONE
            ivBooks?.visibility = View.VISIBLE
            ivThreeLinesMenu?.visibility = View.VISIBLE
            chapterSpinner?.visibility=View.VISIBLE
            tvLeftCornerTitle?.visibility=View.VISIBLE
            tvLeftCornerTitle?.text="Теоретический\nматериал"

        }
        override fun onDestroyView() {
            super.onDestroyView()
            ivBooks = null
            ivThreeLinesMenu= null
            chapterSpinner = null
            tvLeftCornerTitle = null
            tvCenterTitle = null
        }
    }