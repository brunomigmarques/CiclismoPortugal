package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.Prova

@Entity(
    tableName = "provas",
    indices = [androidx.room.Index(value = ["nome", "data", "source"], unique = true)]
)
data class ProvaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val data: Long, // timestamp in millis
    val local: String,
    val tipo: String, // Estrada, BTT, Pista, Ciclocross, Gran Fondo
    val distancias: String, // ex: "50km, 100km, 150km"
    val preco: String, // ex: "15€ - 25€"
    val prazoInscricao: Long?, // timestamp in millis, nullable
    val organizador: String,
    val descricao: String,
    val urlInscricao: String?,
    val inCalendar: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val source: String, // FPC, CiclismoEmPortugal, etc
    val syncedAt: Long = System.currentTimeMillis(),
    val hidden: Boolean = false, // Admin: oculta eventos inválidos/duplicados
    val imageUrl: String? = null // URL da imagem do evento extraída do site
)

fun ProvaEntity.toDomain(): Prova = Prova(
    id = id,
    nome = nome,
    data = data,
    local = local,
    tipo = tipo,
    distancias = distancias,
    preco = preco,
    prazoInscricao = prazoInscricao,
    organizador = organizador,
    descricao = descricao,
    urlInscricao = urlInscricao,
    inCalendar = inCalendar,
    notificationsEnabled = notificationsEnabled,
    source = source,
    hidden = hidden,
    imageUrl = imageUrl
)
