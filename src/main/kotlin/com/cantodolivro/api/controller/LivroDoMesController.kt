package com.cantodolivro.api.controller

import com.cantodolivro.api.model.LivroDoMes
import com.cantodolivro.api.repository.LivroDoMesRepository
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/public/livro-do-mes")
@CrossOrigin(origins = ["http://localhost:3000"])
class LivroDoMesController(
    private val repository: LivroDoMesRepository
) {

    @GetMapping
    fun getLivroDoMes(): LivroDoMes {
        return repository.findTopByOrderByIdDesc() ?: repository.save(
            LivroDoMes(
                mes = "Outubro",
                titulo = "O Morro dos Ventos Uivantes",
                autor = "Emily Brontë",
                indicacao = "Marina",
                capaUrl = "/livro-do-mes.png",
                mediaClube = 4.5
            )
        )
    }

    @PostMapping
    fun salvarLivroDoMes(@RequestBody dados: LivroDoMes): LivroDoMes {
        dados.atualizadoEm = LocalDateTime.now()
        return repository.save(dados)
    }
}
