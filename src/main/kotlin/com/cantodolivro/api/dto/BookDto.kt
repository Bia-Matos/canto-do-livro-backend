package com.cantodolivro.api.dto

data class BookSearchResultDto(
    val externalId: String? = null,
    val titulo: String,
    val autores: List<String> = emptyList(),
    val anoPublicacao: Int? = null,
    val dataPublicacao: String? = null,
    val editora: String? = null,
    val numeroPaginas: Int? = null,
    val isbn: String? = null,
    val isbn10: String? = null,
    val isbn13: String? = null,
    val capaUrl: String? = null,
    val sinopse: String? = null,
    val provider: String = "OPEN_LIBRARY",
    var jaCadastrado: Boolean = false,
    var livroId: Long? = null
)

data class AdicionarLivroRequest(
    val externalId: String? = null,
    val titulo: String,
    val autor: String,
    val sinopse: String? = null,
    val capaUrl: String? = null,
    val isbn: String? = null,
    val isbn10: String? = null,
    val isbn13: String? = null,
    val dataPublicacao: String? = null,
    val anoPublicacao: Int? = null,
    val editora: String? = null,
    val numeroPaginas: Int? = null,
    val provider: String = "OPEN_LIBRARY",
    val indicadoPor: String? = null,
    val indicadoPorEmail: String? = null,
    val status: String = "SUGESTAO"
)
