package com.example.groupprojectfirsttry

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFPictureData
import java.io.ByteArrayInputStream
import java.io.InputStream

class TheoriaFragment : Fragment(R.layout.fragment_theoria) {

    private var ivBooks: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ivBooks = requireActivity().findViewById(R.id.imageViewBooks)
        ivBooks?.visibility = View.VISIBLE
        val view = inflater.inflate(R.layout.fragment_theoria, container, false)
        val containerLayout: LinearLayout = view.findViewById(R.id.LinearLayoutTheoria)

        CoroutineScope(Dispatchers.IO).launch {
            val inputStream: InputStream = requireContext().assets.open("test2.docx")
            val document = XWPFDocument(inputStream)

            withContext(Dispatchers.Main) {
                for (paragraph in document.paragraphs) {
                    val textView = TextView(requireContext())
                    val spannableString = SpannableStringBuilder()

                    for (run in paragraph.runs) {
                        val text = run.text()
                        if (text.isNotEmpty()) {
                            val start = spannableString.length
                            spannableString.append(text)
                            val end = spannableString.length

                            if (run.isBold) {
                                spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            if (run.isItalic) {
                                spannableString.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            // Добавьте другие стили, если необходимо
                        }
                    }

                    textView.text = spannableString
                    textView.textSize = 16f
                    containerLayout.addView(textView)

                    // Обработка изображений в абзаце
                    for (run in paragraph.runs) {
                        val pictures = run.embeddedPictures
                        for (picture in pictures) {
                            val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(picture.pictureData.data))
                            val imageView = ImageView(requireContext())
                            imageView.setImageBitmap(bitmap)
                            imageView.adjustViewBounds = true
                            containerLayout.addView(imageView)
                        }
                    }
                }
            }

            // Закрываем потоки
            document.close()
            inputStream.close()
        }

        return view
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