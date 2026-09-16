package com.cantodolivro.api.dto

data class MembroParticipanteDto(
    val email: String,
    val nome: String,
    val avatarUrl: String?,
    val quantidadeLivros: Int,
    val bilhetes: Int
)

data class RealizarSorteioRequest(
    val emails: List<String>
)

data class ResultadoSorteioDto(
    val livroId: Long,
    val titulo: String,
    val autor: String,
    val capaUrl: String?,
    val indicadoPor: String?
)
