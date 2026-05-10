package com.example.groupprojectfirsttry.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TokenManager
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.*
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.UUID

class RecommendationsFragment : Fragment() {

    private lateinit var rvWeakTopics: RecyclerView
    private lateinit var rvLearningPath: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var nsvContent: View
    private lateinit var llNoData: View
    
    private lateinit var tvWeakHeader: View
    private lateinit var tvPathHeader: View
    
    private lateinit var cvClusterStatus: View
    private lateinit var llClusterBackground: LinearLayout
    private lateinit var tvClusterTitle: TextView
    private lateinit var tvClusterMessage: TextView
    private lateinit var btnStartAdaptiveTraining: MaterialButton

    private val apiService = ApiClient.apiService
    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recommendations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tokenManager = ApiClient.getTokenManager() ?: TokenManager(requireContext())

        rvWeakTopics = view.findViewById(R.id.rvWeakTopics)
        rvLearningPath = view.findViewById(R.id.rvLearningPath)
        progressBar = view.findViewById(R.id.progressBar)
        nsvContent = view.findViewById(R.id.nsvRecommendations)
        llNoData = view.findViewById(R.id.llNoData)
        
        tvWeakHeader = view.findViewById(R.id.tvWeakTopicsHeader)
        tvPathHeader = view.findViewById(R.id.tvLearningPathHeader)
        
        cvClusterStatus = view.findViewById(R.id.cvClusterStatus)
        llClusterBackground = view.findViewById(R.id.llClusterBackground)
        tvClusterTitle = view.findViewById(R.id.tvClusterTitle)
        tvClusterMessage = view.findViewById(R.id.tvClusterMessage)
        btnStartAdaptiveTraining = view.findViewById(R.id.btnStartAdaptiveTraining)

        rvWeakTopics.layoutManager = LinearLayoutManager(requireContext())
        rvLearningPath.layoutManager = LinearLayoutManager(requireContext())

        btnStartAdaptiveTraining.setOnClickListener {
            // Теперь вместо прямого запуска открываем список всех тренажеров
            (requireActivity() as? SecondActivityWithBottomNavMenu)
                ?.replaceFragment(TrainingListFragment(), null)
        }

        loadData()
    }

    private fun loadData() {
        val user = (activity as? UserProvider)?.getUser() ?: return
        val userId = user.id ?: return

        progressBar.isVisible = true
        nsvContent.isVisible = false
        llNoData.isVisible = false
        btnStartAdaptiveTraining.isVisible = false

        lifecycleScope.launch {
            try {
                setupClusterUI(user.clusterId)
                
                val weakResponse = apiService.analyzeWeakTopics(userId)
                val pathResponse = apiService.getLearningPath(userId)

                if (isAdded) {
                    val weakTopics = weakResponse.body()?.weakTopics ?: emptyList()
                    val learningPathSteps = pathResponse.body()?.steps ?: emptyList()

                    val hasData = weakTopics.isNotEmpty() || learningPathSteps.isNotEmpty() || user.clusterId != null
                    
                    if (hasData) {
                        rvWeakTopics.adapter = WeakTopicsAdapter(weakTopics)
                        tvWeakHeader.isVisible = weakTopics.isNotEmpty()
                        
                        rvLearningPath.adapter = LearningPathAdapter(learningPathSteps)
                        tvPathHeader.isVisible = learningPathSteps.isNotEmpty()
                        
                        nsvContent.isVisible = true
                        btnStartAdaptiveTraining.isVisible = true
                    } else {
                        llNoData.isVisible = true
                    }
                    progressBar.isVisible = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    progressBar.isVisible = false
                    llNoData.isVisible = true
                }
            }
        }
    }
    
    private fun setupClusterUI(clusterId: Int?) {
        if (clusterId == null) {
            cvClusterStatus.isVisible = false
            return
        }
        
        cvClusterStatus.isVisible = true
        when (clusterId) {
            1 -> {
                tvClusterTitle.text = "Продвинутый уровень 🏆"
                tvClusterMessage.text = "Вы отлично справляетесь! Вам доступны задачи повышенной сложности и углубленные материалы."
                llClusterBackground.setBackgroundColor(Color.parseColor("#E8F5E9"))
                tvClusterTitle.setTextColor(Color.parseColor("#2E7D32"))
            }
            3 -> {
                tvClusterTitle.text = "Требуется повторение 📚"
                tvClusterMessage.text = "Рекомендуем повторить теоретический материал по выделенным темам перед выполнением новых тестов."
                llClusterBackground.setBackgroundColor(Color.parseColor("#FFF3E0"))
                tvClusterTitle.setTextColor(Color.parseColor("#E65100"))
            }
            else -> {
                tvClusterTitle.text = "Стабильный прогресс 👍"
                tvClusterMessage.text = "Продолжайте обучение в своем темпе. Система подготовила для вас оптимальную подборку материалов."
                llClusterBackground.setBackgroundColor(Color.parseColor("#F0F7FF"))
                tvClusterTitle.setTextColor(Color.parseColor("#1976D2"))
            }
        }
    }

    private inner class WeakTopicsAdapter(private val items: List<WeakTopic>) :
        RecyclerView.Adapter<WeakTopicsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTopic: TextView = view.findViewById(R.id.tvTopicName)
            val pbError: ProgressBar = view.findViewById(R.id.pbErrorRate)
            val tvPercent: TextView = view.findViewById(R.id.tvErrorPercent)
            val tvStats: TextView = view.findViewById(R.id.tvStats)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weak_topic, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTopic.text = item.topic
            val percent = (item.errorRate * 100).toInt()
            holder.pbError.progress = percent
            holder.tvPercent.text = "$percent%"
            holder.tvStats.text = "Ошибок: ${item.errorCount} из ${item.totalAttempts} вопросов"
        }

        override fun getItemCount() = items.size
    }

    private inner class LearningPathAdapter(private val items: List<LearningStep>) :
        RecyclerView.Adapter<LearningPathAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvStep: TextView = view.findViewById(R.id.tvStepNumber)
            val tvAction: TextView = view.findViewById(R.id.tvAction)
            val tvTopic: TextView = view.findViewById(R.id.tvTopicPath)
            val tvContent: TextView = view.findViewById(R.id.tvContentPath)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_learning_step, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvStep.text = item.step.toString()
            holder.tvAction.text = item.action.uppercase()
            holder.tvTopic.text = item.topic
            
            val content = when(item.action.lowercase()) {
                "study" -> "Изучите теорию по теме"
                "watch" -> "Посмотрите видеоматериал"
                "practice" -> "Решите практические задачи (${item.questionIds?.size ?: 0} шт.)"
                else -> ""
            }
            holder.tvContent.text = content
        }

        override fun getItemCount() = items.size
    }
}
