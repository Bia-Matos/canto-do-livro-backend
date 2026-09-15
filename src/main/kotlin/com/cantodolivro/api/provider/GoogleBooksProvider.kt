package com.cantodolivro.api.provider

import com.cantodolivro.api.dto.BookSearchResultDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode

@Component
class GoogleBooksProvider(
    @Value("\${google.books.api-key}")
    private val apiKey: String
) : BookProvider {

    private val logger = LoggerFactory.getLogger(GoogleBooksProvider::class.java)

    override val providerName: String = "GOOGLE_BOOKS"

    private val restClient = RestClient.builder()
        .baseUrl("https://www.googleapis.com/books/v1")
        .build()

    override fun search(query: String): List<BookSearchResultDto> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            val response = restClient.get()
                .uri("/volumes?q={query}&key={key}&maxResults=15&langRestrict=pt,en", trimmed, apiKey)
                .retrieve()
                .body(JsonNode::class.java) ?: return emptyList()

            val items = response.get("items") ?: return emptyList()
            val results = mutableListOf<BookSearchResultDto>()

            for (item in items) {
                val volumeInfo = item.get("volumeInfo") ?: continue
                val title = volumeInfo.get("title")?.asText() ?: continue
                val id = item.get("id")?.asText() ?: continue

                val autores = mutableListOf<String>()
                volumeInfo.get("authors")?.forEach { autores.add(it.asText()) }

                val publishedDate = volumeInfo.get("publishedDate")?.asText()
                val ano = extractYear(publishedDate)

                val publisher = volumeInfo.get("publisher")?.asText()
                val pageCount = volumeInfo.get("pageCount")?.asInt()
                val description = volumeInfo.get("description")?.asText()

                val imageLinks = volumeInfo.get("imageLinks")
                val capaUrl = imageLinks?.get("thumbnail")?.asText()?.replace("http://", "https://")

                val isbns = mutableListOf<String>()
                val isbn10 = volumeInfo.get("industryIdentifiers")?.let { ids ->
                    ids.find { it.get("type")?.asText() == "ISBN_10" }?.get("identifier")?.asText()
                }
                val isbn13 = volumeInfo.get("industryIdentifiers")?.let { ids ->
                    ids.find { it.get("type")?.asText() == "ISBN_13" }?.get("identifier")?.asText()
                }
                val isbnPrincipal = isbn13 ?: isbn10

                results.add(
                    BookSearchResultDto(
                        externalId = id,
                        titulo = title,
                        autores = autores,
                        anoPublicacao = ano,
                        dataPublicacao = publishedDate,
                        editora = publisher,
                        numeroPaginas = pageCount,
                        isbn = isbnPrincipal,
                        isbn10 = isbn10,
                        isbn13 = isbn13,
                        capaUrl = capaUrl,
                        sinopse = description,
                        provider = providerName
                    )
                )
            }

            results
        } catch (e: Exception) {
            logger.error("Erro ao buscar livros no Google Books com termo '$query': ${e.message}", e)
            emptyList()
        }
    }

    override fun getByIsbn(isbn: String): BookSearchResultDto? {
        val normalized = normalizeIsbn(isbn)
        if (normalized.isEmpty()) return null

        return try {
            val response = restClient.get()
                .uri("/volumes?q=isbn:{isbn}&key={key}", normalized, apiKey)
                .retrieve()
                .body(JsonNode::class.java) ?: return null

            val items = response.get("items")
            if (items == null || items.isEmpty) return null

            val item = items[0]
            val volumeInfo = item.get("volumeInfo") ?: return null
            val title = volumeInfo.get("title")?.asText() ?: return null
            val id = item.get("id")?.asText() ?: return null

            val autores = mutableListOf<String>()
            volumeInfo.get("authors")?.forEach { autores.add(it.asText()) }

            val publishedDate = volumeInfo.get("publishedDate")?.asText()
            val ano = extractYear(publishedDate)

            val publisher = volumeInfo.get("publisher")?.asText()
            val pageCount = volumeInfo.get("pageCount")?.asInt()
            val description = volumeInfo.get("description")?.asText()

            val imageLinks = volumeInfo.get("imageLinks")
            val capaUrl = imageLinks?.get("thumbnail")?.asText()?.replace("http://", "https://")

            val isbn10 = volumeInfo.get("industryIdentifiers")?.let { ids ->
                ids.find { it.get("type")?.asText() == "ISBN_10" }?.get("identifier")?.asText()
            }
            val isbn13 = volumeInfo.get("industryIdentifiers")?.let { ids ->
                ids.find { it.get("type")?.asText() == "ISBN_13" }?.get("identifier")?.asText()
            }
            val isbnPrincipal = isbn13 ?: isbn10

            BookSearchResultDto(
                externalId = id,
                titulo = title,
                autores = autores,
                anoPublicacao = ano,
                dataPublicacao = publishedDate,
                editora = publisher,
                numeroPaginas = pageCount,
                isbn = isbnPrincipal,
                isbn10 = isbn10,
                isbn13 = isbn13,
                capaUrl = capaUrl,
                sinopse = description,
                provider = providerName
            )
        } catch (e: Exception) {
            logger.warn("Livro não encontrado para ISBN '$isbn': ${e.message}")
            null
        }
    }

    override fun getByExternalId(externalId: String): BookSearchResultDto? {
        return try {
            val response = restClient.get()
                .uri("/volumes/{id}?key={key}", externalId, apiKey)
                .retrieve()
                .body(JsonNode::class.java) ?: return null

            val volumeInfo = response.get("volumeInfo") ?: return null
            val title = volumeInfo.get("title")?.asText() ?: return null

            val autores = mutableListOf<String>()
            volumeInfo.get("authors")?.forEach { autores.add(it.asText()) }

            val publishedDate = volumeInfo.get("publishedDate")?.asText()
            val ano = extractYear(publishedDate)

            val publisher = volumeInfo.get("publisher")?.asText()
            val pageCount = volumeInfo.get("pageCount")?.asInt()
            val description = volumeInfo.get("description")?.asText()

            val imageLinks = volumeInfo.get("imageLinks")
            val capaUrl = imageLinks?.get("thumbnail")?.asText()?.replace("http://", "https://")

            val isbn10 = volumeInfo.get("industryIdentifiers")?.let { ids ->
                ids.find { it.get("type")?.asText() == "ISBN_10" }?.get("identifier")?.asText()
            }
            val isbn13 = volumeInfo.get("industryIdentifiers")?.let { ids ->
                ids.find { it.get("type")?.asText() == "ISBN_13" }?.get("identifier")?.asText()
            }
            val isbnPrincipal = isbn13 ?: isbn10

            BookSearchResultDto(
                externalId = externalId,
                titulo = title,
                autores = autores,
                anoPublicacao = ano,
                dataPublicacao = publishedDate,
                editora = publisher,
                numeroPaginas = pageCount,
                isbn = isbnPrincipal,
                isbn10 = isbn10,
                isbn13 = isbn13,
                capaUrl = capaUrl,
                sinopse = description,
                provider = providerName
            )
        } catch (e: Exception) {
            logger.error("Erro ao buscar detalhes do livro '$externalId': ${e.message}")
            null
        }
    }

    private fun normalizeIsbn(input: String): String {
        return input.replace("-", "").replace(" ", "").trim()
    }

    private fun extractYear(dateStr: String?): Int? {
        if (dateStr == null) return null
        val match = Regex("""\b(19\d{2}|20\d{2})\b""").find(dateStr)
        return match?.value?.toIntOrNull()
    }
}
