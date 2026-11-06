package com.example.coflow.models

import java.util.Date

data class UserModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val createdAt: Date? = null,
    val isDeleted: Boolean = false,
)
