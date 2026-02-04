package com.ciclismo.portugal.presentation.fantasy.createteam

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.R
import com.ciclismo.portugal.presentation.theme.AppImages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(
    onNavigateBack: () -> Unit,
    onTeamNameConfirmed: (String) -> Unit,
    viewModel: CreateTeamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val teamName by viewModel.teamName.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with cycling image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            // Background cycling image
            AsyncImage(
                model = AppImages.FANTASY_HEADER,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Header content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Criar Equipa",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Começa a tua aventura no Fantasy",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        when (uiState) {
            is CreateTeamUiState.AlreadyHasTeam -> {
                AlreadyHasTeamContent(
                    team = (uiState as CreateTeamUiState.AlreadyHasTeam).team,
                    onNavigateBack = onNavigateBack
                )
            }

            is CreateTeamUiState.Creating -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("A criar equipa...")
                    }
                }
            }

            else -> {
                CreateTeamContent(
                    teamName = teamName,
                    errorMessage = errorMessage,
                    onTeamNameChange = viewModel::onTeamNameChange,
                    onContinue = {
                        // Validate name and continue to wizard
                        if (teamName.length >= 3) {
                            onTeamNameConfirmed(teamName)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CreateTeamContent(
    teamName: String,
    errorMessage: String?,
    onTeamNameChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Icon
        Icon(
            Icons.Default.Groups,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF006600)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Cria a tua equipa!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Escolhe um nome e comeca a construir o teu plantel.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Team name input
        OutlinedTextField(
            value = teamName,
            onValueChange = onTeamNameChange,
            label = { Text("Nome da Equipa") },
            placeholder = { Text("Ex: Ciclistas do Porto") },
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = errorMessage != null,
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                } else {
                    Text(
                        "${teamName.length}/30 - Minimo 3 caracteres",
                        color = if (teamName.length >= 3) Color(0xFF2ECC71) else Color.Gray
                    )
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                InfoRow(icon = Icons.Default.AccountBalanceWallet, text = "Orcamento: 100M")
                InfoRow(icon = Icons.Default.People, text = "Plantel: 15 ciclistas")
                InfoRow(icon = Icons.Default.PlayArrow, text = "Ativos: 8 por corrida")
                InfoRow(icon = Icons.Default.Star, text = "Capitao: pontos x2")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // CONTINUE BUTTON - Goes to wizard
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = teamName.trim().length >= 3,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF006600),
                disabledContainerColor = Color.Gray
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CONTINUAR",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AlreadyHasTeamContent(
    team: com.ciclismo.portugal.domain.model.FantasyTeam,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF2ECC71)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ja tens uma equipa!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = team.teamName,
            fontSize = 18.sp,
            color = Color(0xFF006600),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Vai a 'Minha Equipa' para gerir o teu plantel ou ao 'Mercado' para fazer transferencias.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}
