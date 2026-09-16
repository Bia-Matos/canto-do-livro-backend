package com.cantodolivro.api.repository

import com.cantodolivro.api.model.Membro
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MembroRepository : JpaRepository<Membro, Long> {
    fun findByEmail(email: String): Membro?
}
