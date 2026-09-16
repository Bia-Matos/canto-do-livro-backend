package com.cantodolivro.api.controller

import com.cantodolivro.api.model.LivroDoMes
import com.cantodolivro.api.repository.LivroDoMesRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/public/livro-do-mes")
class LivroDoMesController(
    private val repository: LivroDoMesRepository
) {

    @GetMapping
    fun getLivroDoMes(): ResponseEntity<LivroDoMes> {
        val livro = repository.findTopByOrderByIdDesc()
        return if (livro != null) ResponseEntity.ok(livro) else ResponseEntity.noContent().build()
    }

    @PostMapping
    fun salvarLivroDoMes(@RequestBody dados: LivroDoMes): LivroDoMes {
        dados.atualizadoEm = LocalDateTime.now()
        return repository.save(dados)
    }
}
