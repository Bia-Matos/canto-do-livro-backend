package com.cantodolivro.api.controller

import com.cantodolivro.api.model.HistoricoLivro
import com.cantodolivro.api.model.LivroDoMes
import com.cantodolivro.api.repository.AvaliacaoRepository
import com.cantodolivro.api.repository.HistoricoLivroRepository
import com.cantodolivro.api.repository.LivroDoMesRepository
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/public/historico")
@CrossOrigin(origins = ["http://localhost:3000"])
class HistoricoController(
    private val historicoRepository: HistoricoLivroRepository,
    private val livroDoMesRepository: LivroDoMesRepository,
    private val avaliacaoRepository: AvaliacaoRepository
) {

    @GetMapping
    fun listarHistorico(): List<HistoricoLivro> {
        return historicoRepository.findAllByOrderByIdDesc()
    }

    @PostMapping("/finalizar-mes")
    fun finalizarMes(): HistoricoLivro? {
        val atual = livroDoMesRepository.findTopByOrderByIdDesc() ?: return null
        val avaliacoes = avaliacaoRepository.findAll()

        // 1. Arquivar livro atual no histórico
        val historico = HistoricoLivro(
            mes = atual.mes,
            titulo = atual.titulo,
            autor = atual.autor,
            indicacao = atual.indicacao,
            capaUrl = atual.capaUrl,
            mediaClube = atual.mediaClube,
            totalAvaliacoes = avaliacoes.size,
            finalizadoEm = LocalDateTime.now()
        )
        val salvo = historicoRepository.save(historico)

        // 2. Limpar avaliações para o novo ciclo
        avaliacaoRepository.deleteAll()

        // 3. Atualizar livro do mês para próximo ciclo em branco
        atual.mes = "Novo Mês"
        atual.titulo = "A Definir"
        atual.autor = ""
        atual.indicacao = ""
        atual.mediaClube = 0.0
        atual.capaUrl = "/livro-do-mes.png"
        atual.atualizadoEm = LocalDateTime.now()
        livroDoMesRepository.save(atual)

        return salvo
    }
}
