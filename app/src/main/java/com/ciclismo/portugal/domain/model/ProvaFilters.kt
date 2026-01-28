package com.ciclismo.portugal.domain.model

data class ProvaFilters(
    val tipo: String? = null,  // null = todos
    val local: String? = null, // null = todos
    val dataInicio: Long? = null,
    val dataFim: Long? = null
) {
    fun isActive(): Boolean {
        return tipo != null || local != null || dataInicio != null || dataFim != null
    }

    fun matchesProva(prova: Prova): Boolean {
        // Filtro por tipo
        if (tipo != null && tipo != "Todos" && prova.tipo != tipo) {
            return false
        }

        // Filtro por local
        if (local != null && local != "Todos" && !prova.local.contains(local, ignoreCase = true)) {
            return false
        }

        // Filtro por data início
        if (dataInicio != null && prova.data < dataInicio) {
            return false
        }

        // Filtro por data fim
        if (dataFim != null && prova.data > dataFim) {
            return false
        }

        return true
    }

    companion object {
        fun empty() = ProvaFilters()
    }
}
