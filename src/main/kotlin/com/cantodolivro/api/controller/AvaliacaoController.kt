package com.cantodolivro.api.controller

import com.cantodolivro.api.model.Avaliacao
import com.cantodolivro.api.repository.AvaliacaoRepository
import com.cantodolivro.api.repository.LivroDoMesRepository
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/public/avaliacoes")
@CrossOrigin(origins = ["http://localhost:3000"])
class AvaliacaoController(
    private val avaliacaoRepository: AvaliacaoRepository,
    private val livroDoMesRepository: LivroDoMesRepository
) {

    @GetMapping
    fun listarAvaliacoes(): List<Avaliacao> {
        return avaliacaoRepository.findAllByOrderByIdDesc()
    }

    @PostMapping
    fun criarAvaliacao(@RequestBody avaliacao: Avaliacao): Avaliacao {
        avaliacao.criadoEm = LocalDateTime.now()
        val salva = avaliacaoRepository.save(avaliacao)

        // Recalcular a média do clube para o livro do mês atual
        val todas = avaliacaoRepository.findAll()
        if (todas.isNotEmpty()) {
            val media = todas.map { it.nota }.average()
            val arredondada = Math.round(media * 10.0) / 10.0
            val livro = livroDoMesRepository.findTopByOrderByIdDesc()
            if (livro != null) {
                livro.mediaClube = arredondada
                livroDoMesRepository.save(livro)
            }
        }

        return salva
    }
}
