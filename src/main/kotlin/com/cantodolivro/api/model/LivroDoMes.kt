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
    var mes: String = "Outubro",

    @Column(nullable = false)
    var titulo: String = "O Morro dos Ventos Uivantes",

    @Column(nullable = false)
    var autor: String = "Emily Brontë",

    @Column(nullable = false)
    var indicacao: String = "Marina",

    @Column(columnDefinition = "TEXT")
    var capaUrl: String = "/livro-do-mes.png",

    var mediaClube: Double = 4.5,

    var atualizadoEm: LocalDateTime = LocalDateTime.now()
)
