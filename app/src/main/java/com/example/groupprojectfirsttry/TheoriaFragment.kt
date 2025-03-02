    package com.example.groupprojectfirsttry

    import android.graphics.BitmapFactory
    import android.os.Bundle
    import android.text.SpannableStringBuilder
    import android.text.style.LeadingMarginSpan
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.AdapterView
    import android.widget.ArrayAdapter
    import android.widget.ImageView
    import android.widget.Spinner
    import android.widget.Toast
    import androidx.fragment.app.Fragment
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import org.apache.poi.xwpf.usermodel.XWPFDocument
    import org.apache.poi.xwpf.usermodel.XWPFPictureData
    import java.io.ByteArrayInputStream
    import java.io.InputStream

    class TheoriaFragment : Fragment(R.layout.fragment_theoria) {
        private var ivBooks: ImageView? = null
        private var chapterSpinner: Spinner? = null
        private lateinit var adapter: TheoriaAdapter
        private lateinit var recyclerView: RecyclerView
        private var isLoading = false
        private var isFileProcessed = false // Флаг для проверки, был ли файл полностью прочитан
        private val chapters = arrayOf(
            "0 Введение",
            "1 Основы языка разметки HTML",
            "2 Работа с формами в HTML5",
            "3 Семантическая верстка страниц в HTML5",
            "4 Работа с каскадными таблицами стилей",
            "5 Фильтры в CSS",
            "6 Блоковые элементы в CSS",
            "7 Трансформации, переходы и анимации",
            "8 Адаптивная верстка",
            "9 Создание гибкого макета страницы с помощью Flexbox",
            "10 Двумерная система сеток Grid Layout",
            "11 Использование переменных в CSS",
            "99 Заключение"
        )
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            ivBooks = requireActivity().findViewById(R.id.imageViewBooks)
            ivBooks?.visibility = View.VISIBLE

            chapterSpinner = requireActivity().findViewById(R.id.chapterSpinner)
            chapterSpinner?.visibility=View.VISIBLE
            //
            //  SPINNER
            //

            val adapterSpinner = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                chapters
            )
            adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            chapterSpinner?.adapter =adapterSpinner
            chapterSpinner?.onItemSelectedListener  = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    // Получаем выбранную главу
                    val selectedChapter = chapters[position]
                    loadChapter(selectedChapter)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }




            val view = inflater.inflate(R.layout.fragment_theoria, container, false)
            recyclerView = view.findViewById(R.id.recyclerView)
            adapter = TheoriaAdapter()
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            // Загружаем первую порцию данных
            //loadMoreData()

            // Добавляем слушатель прокрутки
            /*recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val totalItemCount = layoutManager.itemCount

                    // Загружаем больше данных, если пользователь прокрутил до конца
                    if (!isLoading && !isFileProcessed && lastVisibleItemPosition >= totalItemCount - 5) {
                        loadMoreData()
                    }
                }
            })*/

            return view
        }
        private fun loadChapter(chapterTitle: String) {
            // Определяем имя файла на основе названия главы
            val fileName = when (chapterTitle) {
                "0 Введение" -> "0Vvedenie.docx"
                "1 Основы языка разметки HTML" -> "1VvedenieHTML.docx"
                "2 Работа с формами в HTML5" -> "2RabotaSFormami.docx"
                "3 Семантическая верстка страниц в HTML5" -> "3VerstkaStranits.docx"
                "4 Работа с каскадными таблицами стилей" -> "4CSSCascadeTables.docx"
                "5 Фильтры в CSS" -> "5CSSFilters.docx"
                "6 Блоковые элементы в CSS" -> "6CSSBlockElements.docx"
                "7 Трансформации, переходы и анимации" -> "7TransformationAndAnimation.docx"
                "8 Адаптивная верстка" -> "8AdaptiveVerstka.docx"
                "9 Создание гибкого макета страницы с помощью Flexbox" -> "9FlexibleMaket.docx"
                "10 Двумерная система сеток Grid Layout" -> "10GridLayout.docx"
                "11 Использование переменных в CSS" -> "11UsingPeremenInCSS.docx"
                "99 Заключение" -> "99FinalWords.docx"
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

                    for (paragraph in document.paragraphs) {
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
        private fun loadMoreData() {
            if (isLoading || isFileProcessed) return
            isLoading = true

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val inputStream: InputStream = requireContext().assets.open("finalPosobie.docx")
                    val document = XWPFDocument(inputStream)
                    val newItems = mutableListOf<Any>()

                    for (paragraph in document.paragraphs) {
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

                        if (spannableString.isNotEmpty()) {
                            // Добавляем отступ в начале абзаца
                            val indentedText = addParagraphIndent(spannableString)
                            newItems.add(indentedText)
                            Log.d("TheoriaFragment", "Added text with indent: $indentedText")
                        }

                        // Обработка изображений
                        for (run in paragraph.runs) {
                            val pictures = run.embeddedPictures
                            for (picture in pictures) {
                                val bitmap = BitmapFactory.decodeStream(
                                    ByteArrayInputStream(picture.pictureData.data)
                                )
                                newItems.add(bitmap)
                                Log.d(
                                    "TheoriaFragment",
                                    "Added image with size: ${bitmap.width}x${bitmap.height}"
                                )
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (newItems.isNotEmpty()) {
                            adapter.addItems(newItems)
                        } else {
                            isFileProcessed = true
                        }
                        isLoading = false
                    }

                    document.close()
                    inputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        isFileProcessed = true
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

        override fun onPause() {
            super.onPause()
            ivBooks?.visibility = View.INVISIBLE
            chapterSpinner?.visibility=View.INVISIBLE
        }

        override fun onResume() {
            super.onResume()
            ivBooks?.visibility = View.VISIBLE
            chapterSpinner?.visibility=View.VISIBLE
        }

        override fun onDestroyView() {
            super.onDestroyView()
            ivBooks = null
            chapterSpinner = null// Освобождаем ссылку на ivBooks
        }
    }