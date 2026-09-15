package com.cantodolivro.api.repository

import com.cantodolivro.api.model.Avaliacao
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AvaliacaoRepository : JpaRepository<Avaliacao, Long> {
    fun findByLivroIdOrderByIdDesc(livroId: Long): List<Avaliacao>
    fun findAllByOrderByIdDesc(): List<Avaliacao>
}
