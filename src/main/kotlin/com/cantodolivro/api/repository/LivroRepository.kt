package com.cantodolivro.api.repository

import com.cantodolivro.api.model.Livro
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LivroRepository : JpaRepository<Livro, Long> {
    fun findByIsbn(isbn: String): Livro?
    fun findByIsbn13(isbn13: String): Livro?
    fun findByIsbn10(isbn10: String): Livro?
    fun findByExternalId(externalId: String): Livro?
    fun findAllByOrderByVotosDescIdDesc(): List<Livro>
    fun findByStatusOrderByVotosDescIdDesc(status: String): List<Livro>
}
