package com.cantodolivro.api.dto

data class RegistrarMembroRequest(
    val email: String,
    val nome: String,
    val avatarUrl: String? = null
)
