package com.example.groupprojectfirsttry.api

import java.io.IOException

class NoConnectivityException : IOException() {
    override val message: String
        get() = "Отсутствует подключение к интернету"
}

class ServerUnavailableException : IOException() {
    override val message: String
        get() = "Сервер временно недоступен. Попробуйте позже"
}

class ApiException(val code: Int, override val message: String) : IOException()
