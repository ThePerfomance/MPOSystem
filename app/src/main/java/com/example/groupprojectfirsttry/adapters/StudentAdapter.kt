    package com.example.groupprojectfirsttry.adapters

    import android.os.Bundle
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.LinearLayout
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView
    import com.example.groupprojectfirsttry.R
    import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
    import com.example.groupprojectfirsttry.fragments.TestStudentResult
    import com.example.groupprojectfirsttry.simpleClasses.User
    import java.util.UUID

    class StudentAdapter(
        private val students: List<User>,
        private val testResults: Map<UUID, Int> // Карта для хранения результатов тестов (user_id -> score)
    ) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.student_item, parent, false)
            return StudentViewHolder(view, students) // Передаем список студентов в конструктор
        }

        override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
            val student = students[position]
            holder.bind(student, testResults[student.id] ?: 0) // Передаем оценку из карты результатов
        }

        override fun getItemCount(): Int = students.size

        class StudentViewHolder(
            itemView: View,
            private val students: List<User> // Добавляем список студентов как параметр
        ) : RecyclerView.ViewHolder(itemView) {

            private val studentNumber: TextView = itemView.findViewById(R.id.studentNumber)
            private val studentName: TextView = itemView.findViewById(R.id.studentName)
            private val studentGrade: TextView = itemView.findViewById(R.id.studentGrade)
            private val llStudentResultList: LinearLayout = itemView.findViewById(R.id.LLStudentResultList)

            fun bind(student: User, grade: Int) {
                if ((adapterPosition + 1) % 2 == 0) {
                    llStudentResultList.setBackgroundColor(itemView.context.getColor(R.color.LightBlueForList))
                } else {
                    llStudentResultList.setBackgroundColor(itemView.context.getColor(android.R.color.transparent))
                }
                // Отображаем номер студента
                studentNumber.text = "${adapterPosition + 1}."
                // Отображаем имя и фамилию
                studentName.text = "${student.lastname} ${student.firstname} ${student.patronymic}"
                // Отображаем оценку
                studentGrade.text = if (grade > 0) grade.toString() else "---"

                // Обработка кликов
                itemView.setOnClickListener {
                    val bundle = Bundle().apply {
                        putSerializable("userId", student.id)
                        putParcelableArrayList("students", ArrayList(students)) // Используем переданный список студентов
                    }
                    val fragment = TestStudentResult().apply {
                        arguments = bundle
                    }
                    (itemView.context as SecondActivityWithBottomNavMenu).replaceFragment(fragment, bundle)
                }
            }
        }
    }