package com.cantodolivro.api.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "avaliacoes")
data class Avaliacao(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    var livroId: Long? = null,

    // Preenchido quando o mês é finalizado e a avaliação é arquivada no histórico.
    // Enquanto null, a avaliação pertence ao ciclo/livro do mês atual.
    var historicoLivroId: Long? = null,

    @Column(nullable = false)
    var usuarioNome: String = "Membro do Clube",

    var usuarioEmail: String? = null,

    @Column(columnDefinition = "TEXT")
    var usuarioFoto: String? = null,

    @Column(nullable = false)
    var nota: Double = 5.0,

    @Column(columnDefinition = "TEXT", nullable = false)
    var comentario: String = "",

    var criadoEm: LocalDateTime = LocalDateTime.now()
)
