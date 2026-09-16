package com.cantodolivro.api.service

import com.cantodolivro.api.dto.MembroParticipanteDto
import com.cantodolivro.api.dto.ResultadoSorteioDto
import com.cantodolivro.api.model.LivroDoMes
import com.cantodolivro.api.repository.HistoricoLivroRepository
import com.cantodolivro.api.repository.LivroDoMesRepository
import com.cantodolivro.api.repository.LivroRepository
import com.cantodolivro.api.repository.MembroRepository
import com.cantodolivro.api.util.getMesAtual
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private const val JANELA_HISTORICO = 6
private const val BILHETES_NUNCA_GANHOU = 3
private const val BILHETES_GANHOU_UMA_VEZ = 2
private const val BILHETES_GANHOU_MAIS_DE_UMA_VEZ = 1

@Service
class SorteioService(
    private val livroRepository: LivroRepository,
    private val historicoLivroRepository: HistoricoLivroRepository,
    private val livroDoMesRepository: LivroDoMesRepository,
    private val membroRepository: MembroRepository
) {

    fun getMembrosParticipantes(): List<MembroParticipanteDto> {
        val vitoriasPorNome = contarVitoriasRecentes()
        val livrosDisponiveis = livroRepository.findByStatusOrderByVotosDescIdDesc("SUGESTAO")
            .filter { !it.indicadoPorEmail.isNullOrBlank() }

        val porEmail = livrosDisponiveis.groupBy { it.indicadoPorEmail!! }

        return porEmail.map { (email, livros) ->
            val membro = membroRepository.findByEmail(email)
            val nome = membro?.nome ?: livros.first().indicadoPor ?: email
            val vitorias = vitoriasPorNome[nome.trim().lowercase()] ?: 0
            MembroParticipanteDto(
                email = email,
                nome = nome,
                avatarUrl = membro?.avatarUrl,
                quantidadeLivros = livros.size,
                bilhetes = calcularBilhetes(vitorias)
            )
        }.sortedBy { it.nome.lowercase() }
    }

    @Transactional
    fun realizarSorteio(emails: List<String>): ResultadoSorteioDto {
        require(emails.isNotEmpty()) { "Nenhum participante informado." }

        val livros = livroRepository.findByStatusAndIndicadoPorEmailIn("SUGESTAO", emails)
        require(livros.isNotEmpty()) { "Nenhum livro disponível para os participantes informados." }

        val vitoriasPorNome = contarVitoriasRecentes()
        val livrosPorEmail = livros.groupBy { it.indicadoPorEmail!! }

        // Monta o pool ponderado por pessoa: cada email entra no pool uma vez por bilhete
        val pool = livrosPorEmail.keys.flatMap { email ->
            val membro = membroRepository.findByEmail(email)
            val nome = membro?.nome ?: livrosPorEmail[email]!!.first().indicadoPor ?: email
            val vitorias = vitoriasPorNome[nome.trim().lowercase()] ?: 0
            List(calcularBilhetes(vitorias)) { email }
        }

        val emailSorteado = pool.random()
        // Entre os livros da pessoa sorteada, escolhe um aleatoriamente
        val vencedor = livrosPorEmail[emailSorteado]!!.random()

        vencedor.status = "LIDO"
        livroRepository.save(vencedor)

        val livroDoMesAtual = livroDoMesRepository.findTopByOrderByIdDesc()
        val livroDoMes = (livroDoMesAtual ?: LivroDoMes()).apply {
            mes = getMesAtual()
            titulo = vencedor.titulo
            autor = vencedor.autor
            indicacao = vencedor.indicadoPor ?: ""
            capaUrl = vencedor.capaUrl ?: "/livro-do-mes.png"
            sinopse = vencedor.sinopse
            mediaClube = 0.0
            atualizadoEm = LocalDateTime.now()
        }
        livroDoMesRepository.save(livroDoMes)

        return ResultadoSorteioDto(
            livroId = vencedor.id!!,
            titulo = vencedor.titulo,
            autor = vencedor.autor,
            capaUrl = vencedor.capaUrl,
            indicadoPor = vencedor.indicadoPor
        )
    }

    private fun calcularBilhetes(vitoriasRecentes: Int): Int = when {
        vitoriasRecentes <= 0 -> BILHETES_NUNCA_GANHOU
        vitoriasRecentes == 1 -> BILHETES_GANHOU_UMA_VEZ
        else -> BILHETES_GANHOU_MAIS_DE_UMA_VEZ
    }

    private fun contarVitoriasRecentes(): Map<String, Int> {
        return historicoLivroRepository.findAllByOrderByIdDesc()
            .take(JANELA_HISTORICO)
            .mapNotNull { it.indicacao.trim().lowercase().ifBlank { null } }
            .groupingBy { it }
            .eachCount()
    }
}
