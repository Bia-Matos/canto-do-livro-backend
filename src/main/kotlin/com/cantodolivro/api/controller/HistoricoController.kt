package com.cantodolivro.api.controller

import com.cantodolivro.api.dto.AvaliarHistoricoRequest
import com.cantodolivro.api.dto.CriarHistoricoRequest
import com.cantodolivro.api.model.Avaliacao
import com.cantodolivro.api.model.HistoricoLivro
import com.cantodolivro.api.repository.AvaliacaoRepository
import com.cantodolivro.api.repository.HistoricoLivroRepository
import com.cantodolivro.api.repository.LivroDoMesRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/public/historico")
class HistoricoController(
    private val historicoRepository: HistoricoLivroRepository,
    private val livroDoMesRepository: LivroDoMesRepository,
    private val avaliacaoRepository: AvaliacaoRepository
) {

    @GetMapping
    fun listarHistorico(): List<HistoricoLivro> {
        return historicoRepository.findAllByOrderByFinalizadoEmDesc()
    }

    // Permite inserir manualmente um mês/livro já lido anteriormente (antes do app existir, por exemplo)
    @PostMapping
    fun criarHistorico(@RequestBody req: CriarHistoricoRequest): HistoricoLivro {
        val historico = HistoricoLivro(
            mes = req.mes,
            titulo = req.titulo,
            autor = req.autor,
            indicacao = req.indicacao,
            capaUrl = req.capaUrl,
            sinopse = req.sinopse,
            mediaClube = 0.0,
            totalAvaliacoes = 0,
            // Sem data informada (mês desconhecido) fica no fim da lista, não no topo
            finalizadoEm = req.finalizadoEm?.let { LocalDateTime.parse(it) } ?: LocalDateTime.of(1, 1, 1, 0, 0)
        )
        return historicoRepository.save(historico)
    }

    @PutMapping("/{id}")
    fun editarHistorico(@PathVariable id: Long, @RequestBody req: CriarHistoricoRequest): ResponseEntity<HistoricoLivro> {
        val historico = historicoRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        historico.mes = req.mes
        historico.titulo = req.titulo
        historico.autor = req.autor
        historico.indicacao = req.indicacao
        historico.capaUrl = req.capaUrl
        historico.sinopse = req.sinopse
        historico.finalizadoEm = req.finalizadoEm?.let { LocalDateTime.parse(it) } ?: LocalDateTime.of(1, 1, 1, 0, 0)
        return ResponseEntity.ok(historicoRepository.save(historico))
    }

    @DeleteMapping("/{id}")
    fun removerHistorico(@PathVariable id: Long): ResponseEntity<Void> {
        avaliacaoRepository.deleteAll(avaliacaoRepository.findByHistoricoLivroIdOrderByIdDesc(id))
        historicoRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/avaliacoes")
    fun listarAvaliacoesDoHistorico(@PathVariable id: Long): List<Avaliacao> {
        return avaliacaoRepository.findByHistoricoLivroIdOrderByIdDesc(id)
    }

    // Permite avaliar um mês passado que ainda não recebeu nota/comentário de alguém
    @PostMapping("/{id}/avaliacoes")
    fun avaliarHistorico(@PathVariable id: Long, @RequestBody req: AvaliarHistoricoRequest): ResponseEntity<Avaliacao> {
        val historico = historicoRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()

        val avaliacao = Avaliacao(
            historicoLivroId = id,
            usuarioNome = req.usuarioNome,
            usuarioEmail = req.usuarioEmail,
            usuarioFoto = req.usuarioFoto,
            nota = req.nota,
            comentario = req.comentario,
            criadoEm = LocalDateTime.now()
        )
        val salva = avaliacaoRepository.save(avaliacao)

        // Recalcula a média e o total desse mês específico
        val todasDoMes = avaliacaoRepository.findByHistoricoLivroIdOrderByIdDesc(id)
        historico.mediaClube = Math.round(todasDoMes.map { it.nota }.average() * 10.0) / 10.0
        historico.totalAvaliacoes = todasDoMes.size
        historicoRepository.save(historico)

        return ResponseEntity.ok(salva)
    }

    @PostMapping("/finalizar-mes")
    fun finalizarMes(): HistoricoLivro? {
        val atual = livroDoMesRepository.findTopByOrderByIdDesc() ?: return null
        val avaliacoes = avaliacaoRepository.findByHistoricoLivroIdIsNullOrderByIdDesc()

        // 1. Arquivar livro atual no histórico (incluindo sinopse)
        val historico = HistoricoLivro(
            mes = atual.mes,
            titulo = atual.titulo,
            autor = atual.autor,
            indicacao = atual.indicacao,
            capaUrl = atual.capaUrl,
            sinopse = atual.sinopse,
            mediaClube = atual.mediaClube,
            totalAvaliacoes = avaliacoes.size,
            finalizadoEm = LocalDateTime.now()
        )
        val salvo = historicoRepository.save(historico)

        // 2. Arquivar as avaliações do ciclo junto com o histórico (em vez de apagar)
        avaliacoes.forEach { it.historicoLivroId = salvo.id }
        avaliacaoRepository.saveAll(avaliacoes)

        // 3. Atualizar livro do mês para próximo ciclo em branco
        atual.mes = "Novo Mês"
        atual.titulo = "A Definir"
        atual.autor = ""
        atual.indicacao = ""
        atual.sinopse = null
        atual.mediaClube = 0.0
        atual.capaUrl = "/livro-do-mes.png"
        atual.atualizadoEm = LocalDateTime.now()
        livroDoMesRepository.save(atual)

        return salvo
    }
}
