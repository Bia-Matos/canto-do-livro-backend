package com.cantodolivro.api.dto

data class CriarHistoricoRequest(
    val mes: String,
    val titulo: String,
    val autor: String,
    val indicacao: String = "",
    val capaUrl: String = "/livro-do-mes.png",
    val sinopse: String? = null,
    val finalizadoEm: String? = null
)

data class AvaliarHistoricoRequest(
    val usuarioNome: String,
    val usuarioEmail: String? = null,
    val usuarioFoto: String? = null,
    val nota: Double,
    val comentario: String = ""
)
