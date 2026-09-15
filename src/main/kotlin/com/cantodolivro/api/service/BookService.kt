package com.cantodolivro.api.service

import com.cantodolivro.api.dto.AdicionarLivroRequest
import com.cantodolivro.api.dto.BookSearchResultDto
import com.cantodolivro.api.model.Livro
import com.cantodolivro.api.provider.BookProvider
import com.cantodolivro.api.repository.LivroRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BookService(
    private val providers: List<BookProvider>,
    private val livroRepository: LivroRepository
) {
    private val logger = LoggerFactory.getLogger(BookService::class.java)

    private val googleBooksProvider: BookProvider?
        get() = providers.firstOrNull { it.providerName == "GOOGLE_BOOKS" }

    private val openLibraryProvider: BookProvider?
        get() = providers.firstOrNull { it.providerName == "OPEN_LIBRARY" }

    @Cacheable("book_search", key = "#query")
    fun search(query: String): List<BookSearchResultDto> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        logger.info("Buscando livros por: '$trimmed'")
        logger.info("Google Books Provider: ${googleBooksProvider?.providerName ?: "NÃO ENCONTRADO"}")
        logger.info("Open Library Provider: ${openLibraryProvider?.providerName ?: "NÃO ENCONTRADO"}")

        // 1. Se for busca por ISBN, verifica primeiro no banco local
        val normalizedIsbn = trimmed.replace("-", "").replace(" ", "")
        if (normalizedIsbn.length in listOf(10, 13)) {
            val localBook = livroRepository.findByIsbn(normalizedIsbn)
                ?: livroRepository.findByIsbn13(normalizedIsbn)
                ?: livroRepository.findByIsbn10(normalizedIsbn)

            if (localBook != null) {
                return listOf(localBook.toDto(jaCadastrado = true))
            }
        }

        // 2. Tenta Google Books primeiro (melhor cobertura), depois fallback para Open Library
        val results = try {
            logger.info("Tentando Google Books...")
            val googleResults = googleBooksProvider?.search(trimmed) ?: emptyList()
            logger.info("Google Books retornou ${googleResults.size} resultados")
            if (googleResults.isNotEmpty()) {
                googleResults
            } else {
                logger.info("Tentando Open Library...")
                openLibraryProvider?.search(trimmed) ?: emptyList()
            }
        } catch (e: Exception) {
            logger.error("Erro ao buscar em Google Books/Open Library: ${e.message}", e)
            try {
                logger.info("Fallback para Open Library...")
                openLibraryProvider?.search(trimmed) ?: emptyList()
            } catch (fallbackError: Exception) {
                logger.error("Erro no fallback Open Library: ${fallbackError.message}", fallbackError)
                emptyList()
            }
        }

        // 3. Marca resultados que já estão salvos no banco local (deduplicação)
        results.forEach { result ->
            val local = findExistingLocalBook(result.isbn, result.externalId)
            if (local != null) {
                result.jaCadastrado = true
                result.livroId = local.id
            }
        }

        return results
    }

    @Cacheable("book_isbn", key = "#isbn")
    fun getByIsbn(isbn: String): BookSearchResultDto? {
        val normalized = isbn.replace("-", "").replace(" ", "").trim()

        // 1. Banco local primeiro
        val local = livroRepository.findByIsbn(normalized)
            ?: livroRepository.findByIsbn13(normalized)
            ?: livroRepository.findByIsbn10(normalized)

        if (local != null) {
            return local.toDto(jaCadastrado = true)
        }

        // 2. Tenta Google Books primeiro, depois Open Library
        val result = try {
            val googleResult = googleBooksProvider?.getByIsbn(normalized)
            if (googleResult != null) {
                googleResult
            } else {
                openLibraryProvider?.getByIsbn(normalized)
            }
        } catch (e: Exception) {
            try {
                openLibraryProvider?.getByIsbn(normalized)
            } catch (fallbackError: Exception) {
                null
            }
        }

        if (result != null) {
            val localFound = findExistingLocalBook(result.isbn, result.externalId)
            if (localFound != null) {
                result.jaCadastrado = true
                result.livroId = localFound.id
            }
        }
        return result
    }

    @Cacheable("book_details", key = "#externalId")
    fun getByExternalId(externalId: String): BookSearchResultDto? {
        val local = livroRepository.findByExternalId(externalId)
        if (local != null) {
            return local.toDto(jaCadastrado = true)
        }

        val result = try {
            val googleResult = googleBooksProvider?.getByExternalId(externalId)
            if (googleResult != null) {
                googleResult
            } else {
                openLibraryProvider?.getByExternalId(externalId)
            }
        } catch (e: Exception) {
            try {
                openLibraryProvider?.getByExternalId(externalId)
            } catch (fallbackError: Exception) {
                null
            }
        }

        return result
    }

    @Transactional
    fun salvarLivro(req: AdicionarLivroRequest): Livro {
        // Verificar se já existe no banco (deduplicação por ISBN ou externalId)
        val existente = findExistingLocalBook(req.isbn, req.externalId)
        if (existente != null) {
            // Se já existe, apenas incrementa votos ou atualiza quem indicou caso necessário
            return existente
        }

        val novo = Livro(
            titulo = req.titulo,
            autor = req.autor,
            sinopse = req.sinopse,
            capaUrl = req.capaUrl,
            isbn = req.isbn,
            isbn10 = req.isbn10,
            isbn13 = req.isbn13,
            dataPublicacao = req.dataPublicacao,
            anoPublicacao = req.anoPublicacao,
            editora = req.editora,
            numeroPaginas = req.numeroPaginas,
            externalId = req.externalId,
            provider = req.provider,
            status = req.status,
            indicadoPor = req.indicadoPor,
            votos = 1,
            criadoEm = LocalDateTime.now()
        )

        return livroRepository.save(novo)
    }

    @Transactional
    fun votarLivro(id: Long): Livro {
        val livro = livroRepository.findById(id).orElseThrow {
            IllegalArgumentException("Livro não encontrado com id $id")
        }
        livro.votos += 1
        return livroRepository.save(livro)
    }

    fun listarLivros(): List<Livro> {
        return livroRepository.findAllByOrderByVotosDescIdDesc()
    }

    @Transactional
    fun removerLivro(id: Long) {
        livroRepository.deleteById(id)
    }

    private fun findExistingLocalBook(isbn: String?, externalId: String?): Livro? {
        if (!isbn.isNullOrBlank()) {
            val normalized = isbn.replace("-", "").replace(" ", "").trim()
            val found = livroRepository.findByIsbn(normalized)
                ?: livroRepository.findByIsbn13(normalized)
                ?: livroRepository.findByIsbn10(normalized)
            if (found != null) return found
        }
        if (!externalId.isNullOrBlank()) {
            return livroRepository.findByExternalId(externalId)
        }
        return null
    }

    private fun Livro.toDto(jaCadastrado: Boolean): BookSearchResultDto {
        return BookSearchResultDto(
            externalId = this.externalId,
            titulo = this.titulo,
            autores = listOf(this.autor),
            anoPublicacao = this.anoPublicacao,
            dataPublicacao = this.dataPublicacao,
            editora = this.editora,
            numeroPaginas = this.numeroPaginas,
            isbn = this.isbn,
            isbn10 = this.isbn10,
            isbn13 = this.isbn13,
            capaUrl = this.capaUrl,
            sinopse = this.sinopse,
            provider = this.provider,
            jaCadastrado = jaCadastrado,
            livroId = this.id
        )
    }
}
