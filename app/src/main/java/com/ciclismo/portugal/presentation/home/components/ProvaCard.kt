package com.ciclismo.portugal.presentation.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.presentation.theme.*

@Composable
fun ProvaCard(
    prova: Prova,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Event Image
            prova.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = prova.nome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = prova.nome,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

            Spacer(modifier = Modifier.height(8.dp))

            // Type Chip
            AssistChip(
                onClick = {},
                label = { Text(prova.tipo) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = getProvaTypeColor(prova.tipo)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍 ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = prova.local,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Distances
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🚴 ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = prova.distancias,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
        }
    }
}

@Composable
private fun getProvaTypeColor(tipo: String): androidx.compose.ui.graphics.Color {
    return when (tipo) {
        "Estrada" -> TypeEstrada
        "BTT" -> TypeBTT
        "Pista" -> TypePista
        "Ciclocross" -> TypeCiclocross
        "Gran Fondo" -> TypeGranFondo
        else -> Primary
    }
}
