    package com.example.groupprojectfirsttry

    import android.graphics.BitmapFactory
    import android.os.Bundle
    import android.text.SpannableStringBuilder
    import android.text.style.LeadingMarginSpan
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageView
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
        private lateinit var adapter: TheoriaAdapter
        private lateinit var recyclerView: RecyclerView
        private var isLoading = false
        private var isFileProcessed = false // Флаг для проверки, был ли файл полностью прочитан
        private var currentParagraphIndex = 0

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            ivBooks = requireActivity().findViewById(R.id.imageViewBooks)
            ivBooks?.visibility = View.VISIBLE
            val view = inflater.inflate(R.layout.fragment_theoria, container, false)
            recyclerView = view.findViewById(R.id.recyclerView)
            adapter = TheoriaAdapter()
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            // Загружаем первую порцию данных
            loadMoreData()

            // Добавляем слушатель прокрутки
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
            })

            return view
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
        }

        override fun onResume() {
            super.onResume()
            ivBooks?.visibility = View.VISIBLE
        }

        override fun onDestroyView() {
            super.onDestroyView()
            ivBooks = null // Освобождаем ссылку на ivBooks
        }
    }