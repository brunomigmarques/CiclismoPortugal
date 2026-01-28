package com.ciclismo.portugal.domain.model

data class Prova(
    val id: Long = 0,
    val nome: String,
    val data: Long,
    val local: String,
    val tipo: String,
    val distancias: String,
    val preco: String,
    val prazoInscricao: Long?,
    val organizador: String,
    val descricao: String,
    val urlInscricao: String?,
    val inCalendar: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val source: String,
    val hidden: Boolean = false,
    val imageUrl: String? = null
)
