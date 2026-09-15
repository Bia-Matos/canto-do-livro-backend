package com.cantodolivro.api.repository

import com.cantodolivro.api.model.HistoricoLivro
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HistoricoLivroRepository : JpaRepository<HistoricoLivro, Long> {
    fun findAllByOrderByIdDesc(): List<HistoricoLivro>
}
