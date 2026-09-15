package com.cantodolivro.api.repository

import com.cantodolivro.api.model.LivroDoMes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LivroDoMesRepository : JpaRepository<LivroDoMes, Long> {
    fun findTopByOrderByIdDesc(): LivroDoMes?
}
