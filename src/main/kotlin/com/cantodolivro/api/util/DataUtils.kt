package com.cantodolivro.api.util

import java.time.LocalDate

private val MESES = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)

fun getMesAtual(): String = MESES[LocalDate.now().monthValue - 1]
