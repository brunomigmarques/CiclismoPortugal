package com.ciclismo.portugal.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReminderDialog(
    currentReminderDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedDays by remember { mutableStateOf(currentReminderDays) }
    var customDays by remember { mutableStateOf(if (currentReminderDays !in listOf(1, 3, 7, 14, 30)) currentReminderDays.toString() else "") }
    var showCustomInput by remember { mutableStateOf(currentReminderDays !in listOf(1, 3, 7, 14, 30)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Configurar Lembrete",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Quando deseja ser notificado antes da prova?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Opções pré-definidas
                ReminderOption(
                    label = "1 dia antes",
                    days = 1,
                    selected = selectedDays == 1 && !showCustomInput,
                    onClick = {
                        selectedDays = 1
                        showCustomInput = false
                    }
                )

                ReminderOption(
                    label = "3 dias antes",
                    days = 3,
                    selected = selectedDays == 3 && !showCustomInput,
                    onClick = {
                        selectedDays = 3
                        showCustomInput = false
                    }
                )

                ReminderOption(
                    label = "1 semana antes",
                    days = 7,
                    selected = selectedDays == 7 && !showCustomInput,
                    onClick = {
                        selectedDays = 7
                        showCustomInput = false
                    }
                )

                ReminderOption(
                    label = "2 semanas antes",
                    days = 14,
                    selected = selectedDays == 14 && !showCustomInput,
                    onClick = {
                        selectedDays = 14
                        showCustomInput = false
                    }
                )

                ReminderOption(
                    label = "1 mês antes",
                    days = 30,
                    selected = selectedDays == 30 && !showCustomInput,
                    onClick = {
                        selectedDays = 30
                        showCustomInput = false
                    }
                )

                // Opção personalizada
                ReminderOption(
                    label = "Personalizado",
                    days = if (customDays.isNotBlank()) customDays.toIntOrNull() ?: 7 else 7,
                    selected = showCustomInput,
                    onClick = {
                        showCustomInput = true
                    }
                )

                // Input personalizado
                if (showCustomInput) {
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { value ->
                            if (value.isEmpty() || value.toIntOrNull() != null) {
                                customDays = value
                                value.toIntOrNull()?.let { days ->
                                    if (days in 1..365) {
                                        selectedDays = days
                                    }
                                }
                            }
                        },
                        label = { Text("Dias antes") },
                        placeholder = { Text("Ex: 5") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 4.dp),
                        singleLine = true,
                        isError = customDays.isNotBlank() && (customDays.toIntOrNull() == null || customDays.toInt() !in 1..365)
                    )
                    if (customDays.isNotBlank() && (customDays.toIntOrNull() == null || customDays.toInt() !in 1..365)) {
                        Text(
                            "Entre 1 e 365 dias",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDays = if (showCustomInput) {
                        customDays.toIntOrNull()?.coerceIn(1, 365) ?: 7
                    } else {
                        selectedDays
                    }
                    onConfirm(finalDays)
                },
                enabled = !showCustomInput || (customDays.isNotBlank() && customDays.toIntOrNull() != null && customDays.toInt() in 1..365)
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ReminderOption(
    label: String,
    days: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}
