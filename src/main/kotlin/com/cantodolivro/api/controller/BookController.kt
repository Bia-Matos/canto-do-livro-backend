package com.cantodolivro.api.controller

import com.cantodolivro.api.dto.AdicionarLivroRequest
import com.cantodolivro.api.dto.BookSearchResultDto
import com.cantodolivro.api.model.Livro
import com.cantodolivro.api.service.BookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public/books")
@CrossOrigin(origins = ["http://localhost:3000"])
class BookController(
    private val bookService: BookService
) {

    @GetMapping("/search")
    fun search(@RequestParam query: String): List<BookSearchResultDto> {
        return bookService.search(query)
    }

    @GetMapping("/isbn/{isbn}")
    fun getByIsbn(@PathVariable isbn: String): ResponseEntity<BookSearchResultDto> {
        val book = bookService.getByIsbn(isbn)
        return if (book != null) ResponseEntity.ok(book) else ResponseEntity.notFound().build()
    }

    @GetMapping("/details")
    fun getDetails(@RequestParam externalId: String): ResponseEntity<BookSearchResultDto> {
        val book = bookService.getByExternalId(externalId)
        return if (book != null) ResponseEntity.ok(book) else ResponseEntity.notFound().build()
    }

    @GetMapping
    fun listar(): List<Livro> {
        return bookService.listarLivros()
    }

    @PostMapping
    fun salvar(@RequestBody req: AdicionarLivroRequest): Livro {
        return bookService.salvarLivro(req)
    }

    @PostMapping("/{id}/vote")
    fun votar(@PathVariable id: Long): Livro {
        return bookService.votarLivro(id)
    }

    @DeleteMapping("/{id}")
    fun remover(@PathVariable id: Long): ResponseEntity<Void> {
        bookService.removerLivro(id)
        return ResponseEntity.noContent().build()
    }
}
