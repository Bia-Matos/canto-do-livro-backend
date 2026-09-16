package com.cantodolivro.api.controller

import com.cantodolivro.api.dto.RegistrarMembroRequest
import com.cantodolivro.api.model.Membro
import com.cantodolivro.api.repository.MembroRepository
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/public/membros")
class MembroController(
    private val membroRepository: MembroRepository
) {

    @PostMapping
    fun registrar(@RequestBody req: RegistrarMembroRequest): Membro {
        val existente = membroRepository.findByEmail(req.email)
        val membro = (existente ?: Membro(email = req.email)).apply {
            nome = req.nome
            avatarUrl = req.avatarUrl
            atualizadoEm = LocalDateTime.now()
        }
        return membroRepository.save(membro)
    }
}
