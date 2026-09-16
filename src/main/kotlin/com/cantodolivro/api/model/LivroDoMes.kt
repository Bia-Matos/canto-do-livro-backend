package com.cantodolivro.api.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "livro_do_mes")
data class LivroDoMes(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var mes: String = "",

    @Column(nullable = false)
    var titulo: String = "",

    @Column(nullable = false)
    var autor: String = "",

    @Column(nullable = false)
    var indicacao: String = "",

    @Column(columnDefinition = "TEXT")
    var capaUrl: String = "/livro-do-mes.png",

    @Column(columnDefinition = "TEXT")
    var sinopse: String? = null,

    var mediaClube: Double = 0.0,

    var atualizadoEm: LocalDateTime = LocalDateTime.now()
)
