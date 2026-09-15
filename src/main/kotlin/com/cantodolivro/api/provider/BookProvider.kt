package com.cantodolivro.api.provider

import com.cantodolivro.api.dto.BookSearchResultDto

interface BookProvider {
    val providerName: String

    /**
     * Busca livros por título, autor ou termo geral.
     */
    fun search(query: String): List<BookSearchResultDto>

    /**
     * Busca livro diretamente por ISBN (ISBN-10 ou ISBN-13).
     */
    fun getByIsbn(isbn: String): BookSearchResultDto?

    /**
     * Busca detalhes do livro por identificador do provedor externo.
     */
    fun getByExternalId(externalId: String): BookSearchResultDto?
}
