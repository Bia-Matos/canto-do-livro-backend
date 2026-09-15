package com.cantodolivro.api.provider

import com.cantodolivro.api.dto.BookSearchResultDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode

@Component
class OpenLibraryProvider : BookProvider {

    private val logger = LoggerFactory.getLogger(OpenLibraryProvider::class.java)

    override val providerName: String = "OPEN_LIBRARY"

    private val restClient = RestClient.builder()
        .baseUrl("https://openlibrary.org")
        .defaultHeader("User-Agent", "CantoDoLivro/1.0 (cantodolivro@clube.com)")
        .build()

    override fun search(query: String): List<BookSearchResultDto> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Se for um ISBN, busca direto por ISBN
        val normalizedIsbn = normalizeIsbn(trimmed)
        if (isLikelyIsbn(normalizedIsbn)) {
            val bookByIsbn = getByIsbn(normalizedIsbn)
            if (bookByIsbn != null) {
                return listOf(bookByIsbn)
            }
        }

        return try {
            val response = restClient.get()
                .uri("/search.json?q={query}&limit=15&fields=key,title,author_name,first_publish_year,cover_i,isbn,edition_key,publisher,number_of_pages_median", trimmed)
                .retrieve()
                .body(JsonNode::class.java)

            val docs = response?.get("docs") ?: return emptyList()
            val results = mutableListOf<BookSearchResultDto>()

            for (doc in docs) {
                val title = doc.get("title")?.asText() ?: continue
                
                // Autores
                val autores = mutableListOf<String>()
                doc.get("author_name")?.forEach { autores.add(it.asText()) }

                // Ano e Data
                val ano = doc.get("first_publish_year")?.asInt()
                val dataPub = ano?.toString()

                // Editoras
                val editoras = mutableListOf<String>()
                doc.get("publisher")?.forEach { editoras.add(it.asText()) }
                val editora = editoras.firstOrNull()

                // Páginas
                val paginas = doc.get("number_of_pages_median")?.asInt()

                // ISBNs
                val isbns = mutableListOf<String>()
                doc.get("isbn")?.forEach { isbns.add(it.asText()) }
                val isbn13 = isbns.firstOrNull { it.length == 13 }
                val isbn10 = isbns.firstOrNull { it.length == 10 }
                val isbnPrincipal = isbn13 ?: isbn10 ?: isbns.firstOrNull()

                // Capa
                val coverId = doc.get("cover_i")?.asInt()
                val capaUrl = when {
                    coverId != null && coverId > 0 -> "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
                    isbnPrincipal != null -> "https://covers.openlibrary.org/b/isbn/$isbnPrincipal-L.jpg"
                    else -> null
                }

                // Chave da obra ou edição
                val key = doc.get("key")?.asText() // ex: "/works/OL45883W"

                results.add(
                    BookSearchResultDto(
                        externalId = key,
                        titulo = title,
                        autores = autores,
                        anoPublicacao = ano,
                        dataPublicacao = dataPub,
                        editora = editora,
                        numeroPaginas = paginas,
                        isbn = isbnPrincipal,
                        isbn10 = isbn10,
                        isbn13 = isbn13,
                        capaUrl = capaUrl,
                        sinopse = null,
                        provider = providerName
                    )
                )
            }

            results
        } catch (e: Exception) {
            logger.error("Erro ao buscar livros na Open Library com termo '$query': ${e.message}", e)
            emptyList()
        }
    }

    override fun getByIsbn(isbn: String): BookSearchResultDto? {
        val normalized = normalizeIsbn(isbn)
        if (normalized.isEmpty()) return null

        return try {
            val response = restClient.get()
                .uri("/isbn/{isbn}.json", normalized)
                .retrieve()
                .body(JsonNode::class.java) ?: return null

            val title = response.get("title")?.asText() ?: return null
            val dataPub = response.get("publish_date")?.asText()
            val ano = extractYear(dataPub)
            val paginas = response.get("number_of_pages")?.asInt()

            // Editoras
            val editoras = mutableListOf<String>()
            response.get("publishers")?.forEach { editoras.add(it.asText()) }
            val editora = editoras.firstOrNull()

            // ISBN
            val isbn10 = response.get("isbn_10")?.firstOrNull()?.asText()
            val isbn13 = response.get("isbn_13")?.firstOrNull()?.asText()

            // Capa
            val covers = response.get("covers")
            val coverId = covers?.firstOrNull()?.asInt()
            val capaUrl = when {
                coverId != null && coverId > 0 -> "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
                else -> "https://covers.openlibrary.org/b/isbn/$normalized-L.jpg"
            }

            // Descrição da edição ou obra
            var sinopse = extractDescription(response.get("description"))

            // Se a edição não tiver sinopse, buscar da obra pai
            val works = response.get("works")
            val workKey = works?.firstOrNull()?.get("key")?.asText()
            if (sinopse == null && workKey != null) {
                sinopse = fetchWorkDescription(workKey)
            }

            // Autores
            val autores = mutableListOf<String>()
            response.get("authors")?.forEach {
                val authorKey = it.get("key")?.asText()
                if (authorKey != null) {
                    val authorName = fetchAuthorName(authorKey)
                    if (authorName != null) autores.add(authorName)
                }
            }

            BookSearchResultDto(
                externalId = workKey ?: response.get("key")?.asText(),
                titulo = title,
                autores = autores,
                anoPublicacao = ano,
                dataPublicacao = dataPub,
                editora = editora,
                numeroPaginas = paginas,
                isbn = normalized,
                isbn10 = isbn10,
                isbn13 = isbn13,
                capaUrl = capaUrl,
                sinopse = sinopse,
                provider = providerName
            )
        } catch (e: Exception) {
            logger.warn("Livro não encontrado para ISBN '$isbn': ${e.message}")
            null
        }
    }

    override fun getByExternalId(externalId: String): BookSearchResultDto? {
        val path = if (externalId.startsWith("/")) externalId else "/$externalId"
        return try {
            val response = restClient.get()
                .uri("$path.json")
                .retrieve()
                .body(JsonNode::class.java) ?: return null

            val title = response.get("title")?.asText() ?: return null
            var sinopse = extractDescription(response.get("description"))
            
            val covers = response.get("covers")
            val coverId = covers?.firstOrNull()?.asInt()
            var capaUrl = if (coverId != null && coverId > 0) {
                "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
            } else null

            var isbnPrincipal: String? = null
            var editora: String? = null
            var ano: Int? = null

            // Se a obra não tiver capa ou sinopse direta, vasculha as edições da obra
            try {
                val editionsRes = restClient.get()
                    .uri("$path/editions.json?limit=10")
                    .retrieve()
                    .body(JsonNode::class.java)

                val entries = editionsRes?.get("entries")
                if (entries != null) {
                    for (edition in entries) {
                        if (capaUrl == null) {
                            val edCover = edition.get("covers")?.firstOrNull()?.asInt()
                            if (edCover != null && edCover > 0) {
                                capaUrl = "https://covers.openlibrary.org/b/id/$edCover-L.jpg"
                            }
                        }
                        if (isbnPrincipal == null) {
                            val isbn13 = edition.get("isbn_13")?.firstOrNull()?.asText()
                            val isbn10 = edition.get("isbn_10")?.firstOrNull()?.asText()
                            isbnPrincipal = isbn13 ?: isbn10
                            if (capaUrl == null && isbnPrincipal != null) {
                                capaUrl = "https://covers.openlibrary.org/b/isbn/$isbnPrincipal-L.jpg"
                            }
                        }
                        if (sinopse == null) {
                            sinopse = extractDescription(edition.get("description"))
                                ?: extractDescription(edition.get("notes"))
                        }
                        if (editora == null) {
                            editora = edition.get("publishers")?.firstOrNull()?.asText()
                        }
                        if (ano == null) {
                            ano = extractYear(edition.get("publish_date")?.asText())
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Não foi possível buscar edições para $path: ${e.message}")
            }

            BookSearchResultDto(
                externalId = externalId,
                titulo = title,
                autores = emptyList(),
                sinopse = sinopse,
                capaUrl = capaUrl,
                isbn = isbnPrincipal,
                editora = editora,
                anoPublicacao = ano,
                provider = providerName
            )
        } catch (e: Exception) {
            logger.error("Erro ao buscar detalhes da obra '$externalId': ${e.message}")
            null
        }
    }

    private fun fetchWorkDescription(workKey: String): String? {
        val path = if (workKey.startsWith("/")) workKey else "/$workKey"
        return try {
            val res = restClient.get()
                .uri("$path.json")
                .retrieve()
                .body(JsonNode::class.java)
            extractDescription(res?.get("description"))
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchAuthorName(authorKey: String): String? {
        val path = if (authorKey.startsWith("/")) authorKey else "/$authorKey"
        return try {
            val res = restClient.get()
                .uri("$path.json")
                .retrieve()
                .body(JsonNode::class.java)
            res?.get("name")?.asText()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDescription(node: JsonNode?): String? {
        if (node == null || node.isNull) return null
        return if (node.isTextual) {
            node.asText().ifBlank { null }
        } else if (node.isObject && node.has("value")) {
            node.get("value")?.asText()?.ifBlank { null }
        } else {
            null
        }
    }

    private fun normalizeIsbn(input: String): String {
        return input.replace("-", "").replace(" ", "").trim()
    }

    private fun isLikelyIsbn(normalized: String): Boolean {
        return (normalized.length == 10 && normalized.matches(Regex("^[0-9]{9}[0-9Xx]$"))) ||
               (normalized.length == 13 && normalized.matches(Regex("^[0-9]{13}$")))
    }

    private fun extractYear(dateStr: String?): Int? {
        if (dateStr == null) return null
        val match = Regex("""\b(19\d{2}|20\d{2})\b""").find(dateStr)
        return match?.value?.toIntOrNull()
    }
}
