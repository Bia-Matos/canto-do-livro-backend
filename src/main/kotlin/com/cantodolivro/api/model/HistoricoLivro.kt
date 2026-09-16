package com.cantodolivro.api.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "historico_livros")
data class HistoricoLivro(
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
    var capaUrl: String = "",

    @Column(columnDefinition = "TEXT")
    var sinopse: String? = null,

    var mediaClube: Double = 0.0,

    var totalAvaliacoes: Int = 0,

    var finalizadoEm: LocalDateTime = LocalDateTime.now()
)
