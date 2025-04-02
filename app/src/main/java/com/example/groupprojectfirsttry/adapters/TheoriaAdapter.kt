    package com.example.groupprojectfirsttry.adapters

    import android.graphics.Bitmap
    import android.text.SpannableStringBuilder
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageView
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView
    import com.example.groupprojectfirsttry.R

    class TheoriaAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val items = mutableListOf<Any>() // Может быть String (текст) или Bitmap (изображение)

        fun addItems(newItems: List<Any>) {
            val positionStart = items.size
            items.addAll(newItems)
            notifyItemRangeInserted(positionStart, newItems.size)
        }
        fun setItems(newItems: List<Any>) {
            items.clear() // Очищаем старые элементы
            items.addAll(newItems) // Добавляем новые элементы
            notifyDataSetChanged() // Уведомляем адаптер о изменении данных
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is SpannableStringBuilder -> {
                    TYPE_TEXT
                }
                is Bitmap -> {
                    TYPE_IMAGE
                }
                else -> {
                    throw IllegalArgumentException("Unknown type")
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_TEXT -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_text, parent, false)
                    TextViewHolder(view)
                }
                TYPE_IMAGE -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_image, parent, false)
                    ImageViewHolder(view)
                }
                else -> throw IllegalArgumentException("Unknown view type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is TextViewHolder -> {
                    val item = items[position] as SpannableStringBuilder
                    holder.bind(item)
                }
                is ImageViewHolder -> {
                    val item = items[position] as Bitmap
                    holder.bind(item)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        companion object {
            private const val TYPE_TEXT = 0
            private const val TYPE_IMAGE = 1
        }

        class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val textView: TextView = view.findViewById(R.id.textViewItemText)

            fun bind(text: SpannableStringBuilder) {
                textView.text = text
            }
        }

        class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val imageView: ImageView = view.findViewById(R.id.imageView)

            fun bind(bitmap: Bitmap) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }