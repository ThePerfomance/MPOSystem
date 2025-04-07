package com.example.groupprojectfirsttry.interfaces

import com.example.groupprojectfirsttry.api.Group
import com.example.groupprojectfirsttry.simpleClasses.User

interface UserProvider {
    fun getUser(): User
    suspend fun getUserGroups(): List<Group>?
}