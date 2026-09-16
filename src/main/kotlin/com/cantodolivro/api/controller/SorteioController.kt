package com.cantodolivro.api.controller

import com.cantodolivro.api.dto.MembroParticipanteDto
import com.cantodolivro.api.dto.RealizarSorteioRequest
import com.cantodolivro.api.dto.ResultadoSorteioDto
import com.cantodolivro.api.service.SorteioService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public/sorteio")
class SorteioController(
    private val sorteioService: SorteioService
) {

    @GetMapping("/participantes")
    fun getParticipantes(): List<MembroParticipanteDto> {
        return sorteioService.getMembrosParticipantes()
    }

    @PostMapping("/realizar")
    fun realizarSorteio(@RequestBody request: RealizarSorteioRequest): ResponseEntity<ResultadoSorteioDto> {
        return try {
            ResponseEntity.ok(sorteioService.realizarSorteio(request.emails))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
}
