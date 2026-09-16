package com.cantodolivro.api.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "livros")
data class Livro(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var titulo: String = "",

    @Column(nullable = false)
    var autor: String = "",

    @Column(columnDefinition = "TEXT")
    var sinopse: String? = null,

    @Column(columnDefinition = "TEXT")
    var capaUrl: String? = null,

    var isbn: String? = null,
    var isbn10: String? = null,
    var isbn13: String? = null,
    var dataPublicacao: String? = null,
    var anoPublicacao: Int? = null,
    var editora: String? = null,
    var numeroPaginas: Int? = null,

    var externalId: String? = null,

    @Column(nullable = false)
    var provider: String = "OPEN_LIBRARY",

    @Column(nullable = false)
    var status: String = "SUGESTAO", // SUGESTAO, EM_VOTACAO, LIDO

    var votos: Int = 0,

    var indicadoPor: String? = null,
    var indicadoPorEmail: String? = null,

    var criadoEm: LocalDateTime = LocalDateTime.now()
)
