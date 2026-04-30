package com.example.groupprojectfirsttry.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.groupprojectfirsttry.MainActivity
import com.example.groupprojectfirsttry.R
import com.example.groupprojectfirsttry.SecondActivityWithBottomNavMenu
import com.example.groupprojectfirsttry.api.ApiClient
import com.example.groupprojectfirsttry.api.TokenManager
import com.example.groupprojectfirsttry.simpleClasses.User
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    private lateinit var etSurname: EditText
    private lateinit var etName: EditText
    private lateinit var etPatronymic: EditText
    private lateinit var etGroup: EditText
    private lateinit var tvGroupLabel: TextView
    private lateinit var tvCenterTitle: TextView
    private lateinit var imgExit: ImageView
    private lateinit var viewDividerGroup: View
    
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var profileContent: View
    private lateinit var tokenManager: TokenManager
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tokenManager = TokenManager(requireContext())
        shimmerContainer = view.findViewById(R.id.shimmer_view_container)
        profileContent = view.findViewById(R.id.profile_content)

        etSurname = view.findViewById(R.id.editTextText)
        etName = view.findViewById(R.id.editTextText2)
        etPatronymic = view.findViewById(R.id.editTextText3)
        etGroup = view.findViewById(R.id.editTextText4)
        tvGroupLabel = view.findViewById(R.id.textViewGroup)
        imgExit = view.findViewById(R.id.imageViewExit)
        viewDividerGroup = view.findViewById(R.id.viewDividerGroup)

        tvCenterTitle = requireActivity().findViewById(R.id.textViewUpper)

        // 1. Сначала показываем то, что уже есть в Activity
        val activity = requireActivity() as SecondActivityWithBottomNavMenu
        updateUI(activity.getUser())

        // 2. Загружаем свежие данные с сервера
        refreshUserData()

        return view
    }

    private fun refreshUserData() {
        val activity = requireActivity() as SecondActivityWithBottomNavMenu
        val email = tokenManager.getUserEmail()
        Log.d("ProfileFragment", "Starting refreshUserData for email: $email")

        if (email == null) {
            Log.e("ProfileFragment", "Email is null, cannot refresh data")
            return
        }

        startLoading()
        
        lifecycleScope.launch {
            try {
                Log.d("ProfileFragment", "Requesting user profile from server...")
                val updatedUser = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getUserByEmail(email)
                }
                
                // ЛОГ В ФОРМАТЕ JSON
                val userJson = gson.toJson(updatedUser)
                Log.d("ProfileFragment", "RECEIVED USER JSON: $userJson")
                
                // Обновляем пользователя в Activity, чтобы id и другие поля были актуальны
                activity.updateCurrentUser(updatedUser)

                // Обновляем основные поля профиля
                updateUI(updatedUser)

                // Если студент, загружаем группы
                if (updatedUser.role == "student") {
                    Log.d("ProfileFragment", "User is student, requesting groups for ID: ${updatedUser.id}")
                    val groups = activity.getUserGroups() // Это уже suspend метод
                    
                    // ЛОГ ГРУПП В ФОРМАТЕ JSON
                    val groupsJson = gson.toJson(groups)
                    Log.d("ProfileFragment", "RECEIVED GROUPS JSON: $groupsJson")
                    
                    if (!groups.isNullOrEmpty()) {
                        val groupNames = groups.joinToString(", ") { it.name }
                        Log.d("ProfileFragment", "Setting etGroup text to: $groupNames")
                        etGroup.setText(groupNames)
                    } else {
                        Log.w("ProfileFragment", "Groups list is empty or null")
                        etGroup.setText("Группа не назначена")
                    }
                } else {
                    Log.d("ProfileFragment", "User role is ${updatedUser.role}, skipping group load")
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error in refreshUserData: ${e.message}", e)
            } finally {
                Log.d("ProfileFragment", "Refresh finished, stopping loading")
                stopLoading()
            }
        }
    }

    private fun updateUI(user: User) {
        etSurname.setText(user.lastname)
        etName.setText(user.firstname)
        etPatronymic.setText(user.patronymic)

        if (user.role == "student") {
            tvGroupLabel.visibility = View.VISIBLE
            etGroup.visibility = View.VISIBLE
            viewDividerGroup.visibility = View.VISIBLE
            imgExit.visibility = View.GONE
        } else {
            tvGroupLabel.visibility = View.GONE
            etGroup.visibility = View.GONE
            viewDividerGroup.visibility = View.GONE
            imgExit.visibility = View.VISIBLE
            imgExit.setOnClickListener { showExitDialog() }
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Да") { _, _ ->
                tokenManager.clear()
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun startLoading() {
        shimmerContainer.visibility = View.VISIBLE
        shimmerContainer.startShimmer()
        profileContent.visibility = View.GONE
    }

    private fun stopLoading() {
        if (!isAdded) return
        shimmerContainer.stopShimmer()
        shimmerContainer.visibility = View.GONE
        profileContent.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        tvCenterTitle.text = "Данные профиля"
    }
}
