package com.example.groupprojectfirsttry.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.interfaces.UserProvider
import com.example.groupprojectfirsttry.simpleClasses.*
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class RecommendationsFragment : Fragment() {

    private lateinit var rvWeakTopics: RecyclerView
    private lateinit var rvRecommendations: RecyclerView
    private lateinit var rvLearningPath: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val apiService = ApiClient.apiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recommendations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvWeakTopics = view.findViewById(R.id.rvWeakTopics)
        rvRecommendations = view.findViewById(R.id.rvRecommendations)
        rvLearningPath = view.findViewById(R.id.rvLearningPath)
        progressBar = view.findViewById(R.id.progressBar)

        rvWeakTopics.layoutManager = LinearLayoutManager(requireContext())
        rvRecommendations.layoutManager = LinearLayoutManager(requireContext())
        rvLearningPath.layoutManager = LinearLayoutManager(requireContext())

        loadData()
    }

    private fun loadData() {
        val user = (activity as? UserProvider)?.getUser() ?: return
        val userId = user.id ?: return

        progressBar.isVisible = true

        lifecycleScope.launch {
            try {
                // В новых ML-функциях сначала вызываем анализ слабых тем
                apiService.analyzeWeakTopics(userId)
                
                val weakTopics = apiService.analyzeWeakTopics(userId).body() ?: emptyList()
                val recommendations = apiService.getPersonalizedRecommendations(userId)
                val learningPath = apiService.getLearningPath(userId)

                if (isAdded) {
                    rvWeakTopics.adapter = WeakTopicsAdapter(weakTopics)
                    rvRecommendations.adapter = PersonalizedRecommendationsAdapter(recommendations)
                    rvLearningPath.adapter = LearningPathAdapter(learningPath.steps)
                    progressBar.isVisible = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) progressBar.isVisible = false
            }
        }
    }

    // --- Adapters ---

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
            holder.tvStats.text = "Ошибок: ${item.wrongAnswers} из ${item.totalQuestions} вопросов"
        }

        override fun getItemCount() = items.size
    }

    private inner class PersonalizedRecommendationsAdapter(private val items: List<PersonalizedRecommendation>) :
        RecyclerView.Adapter<PersonalizedRecommendationsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTopic: TextView = view.findViewById(R.id.tvRecommendationTopic)
            val tvPriority: TextView = view.findViewById(R.id.tvPriorityBadge)
            val tvMessage: TextView = view.findViewById(R.id.tvRecommendationMessage)
            val llResources: LinearLayout = view.findViewById(R.id.llResourcesContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recommendation, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTopic.text = item.topic
            holder.tvPriority.text = item.priority
            holder.tvMessage.text = item.message
            
            holder.llResources.removeAllViews()
            item.resources.forEach { res ->
                val btn = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
                btn.text = res.title
                btn.setAllCaps(false)
                btn.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.url))
                    startActivity(intent)
                }
                holder.llResources.addView(btn)
            }
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
            holder.tvContent.text = item.content
        }

        override fun getItemCount() = items.size
    }
}
