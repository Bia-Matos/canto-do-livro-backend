package com.cantodolivro.api.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TestController {

    @GetMapping("/public/status")
    fun publicStatus(): Map<String, Any> {
        return mapOf(
            "status" to "ONLINE",
            "mensagem" to "Canto do Livro API rodando perfeitamente!",
            "timestamp" to System.currentTimeMillis()
        )
    }

    @GetMapping("/me")
    fun getAuthenticatedUser(@AuthenticationPrincipal jwt: Jwt?): Map<String, Any?> {
        if (jwt == null) {
            return mapOf("autenticado" to false)
        }
        return mapOf(
            "autenticado" to true,
            "userId" to jwt.subject,
            "email" to jwt.claims["email"],
            "nome" to (jwt.claims["user_metadata"] as? Map<*, *>)?.get("full_name")
        )
    }
}
