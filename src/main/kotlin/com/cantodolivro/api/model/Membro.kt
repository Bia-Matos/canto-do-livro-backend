package com.cantodolivro.api.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "membros")
data class Membro(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    var email: String = "",

    @Column(nullable = false)
    var nome: String = "",

    @Column(columnDefinition = "TEXT")
    var avatarUrl: String? = null,

    var atualizadoEm: LocalDateTime = LocalDateTime.now()
)
