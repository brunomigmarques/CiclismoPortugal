package com.ciclismo.portugal.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.*

// Attractive cycling images from Unsplash
private object OnboardingImages {
    const val WELCOME = "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=1200&q=80" // Mountain cycling panorama
    const val ROAD = "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=800&q=80" // Professional peloton
    const val BTT = "https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?w=800&q=80" // Mountain bike trail
    const val GRAVEL = "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&q=80" // Gravel/forest path
    const val ALL_TYPES = "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=800&q=80" // General cycling
    const val EXPERIENCE = "https://images.unsplash.com/photo-1502126829571-83575bb53030?w=1200&q=80" // Time trial cyclist
    const val REGION = "https://images.unsplash.com/photo-1471506480208-91b3a4cc78be?w=1200&q=80" // Scenic cycling route
    const val GOALS = "https://images.unsplash.com/photo-1594495894542-a46cc73e081a?w=1200&q=80" // Sprint finish
    const val COMPLETE = "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=1200&q=80" // Celebration
}

private val PortugueseGreen = Color(0xFF006600)
private val PortugueseRed = Color(0xFFCC0000)
private val PortugueseYellow = Color(0xFFFFCC00)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()

    LaunchedEffect(isComplete) {
        if (isComplete) {
            onComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "onboarding_transition"
        ) { step ->
            when (step) {
                OnboardingStep.Welcome -> WelcomeStep(
                    onNext = { viewModel.nextStep() },
                    onSkip = { viewModel.skipOnboarding() }
                )
                OnboardingStep.CyclingType -> CyclingTypeWithExperienceStep(
                    selectedTypes = preferences.cyclingTypes,
                    selectedLevel = preferences.experienceLevel,
                    selectedRegion = preferences.favoriteRegion,
                    onToggleType = { viewModel.toggleCyclingType(it) },
                    onSelectLevel = { viewModel.setExperienceLevel(it) },
                    onSelectRegion = { viewModel.setFavoriteRegion(it) },
                    onNext = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() },
                    onSkip = { viewModel.skipOnboarding() }
                )
                OnboardingStep.Goals -> GoalsStep(
                    selected = preferences.goals,
                    onToggle = { viewModel.toggleGoal(it) },
                    onNext = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() },
                    onSkip = { viewModel.skipOnboarding() }
                )
                OnboardingStep.Complete -> CompleteStep(
                    preferences = preferences,
                    onFinish = { viewModel.completeOnboarding() },
                    onBack = { viewModel.previousStep() }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        AsyncImage(
            model = OnboardingImages.WELCOME,
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo/Title area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ciclismo",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Portugal",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = PortugueseYellow
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "A tua app de ciclismo em Portugal",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }

            // Features preview
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureItem(emoji = "📅", text = "Calendário de provas em todo o país")
                FeatureItem(emoji = "📰", text = "Notícias de ciclismo português")
                FeatureItem(emoji = "🏆", text = "Fantasy Cycling - cria a tua equipa")
                FeatureItem(emoji = "🎥", text = "Vídeos das melhores corridas")
            }

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PortugueseGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Personalizar Experiência",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Saltar por agora",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureItem(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun CyclingTypeWithExperienceStep(
    selectedTypes: Set<CyclingType>,
    selectedLevel: ExperienceLevel,
    selectedRegion: Region,
    onToggleType: (CyclingType) -> Unit,
    onSelectLevel: (ExperienceLevel) -> Unit,
    onSelectRegion: (Region) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    var showRegionDropdown by remember { mutableStateOf(false) }

    OnboardingStepLayout(
        backgroundImage = OnboardingImages.ROAD,
        stepNumber = 1,
        totalSteps = 2,
        title = "Personaliza a tua experiência",
        subtitle = "Podes selecionar vários tipos de ciclismo",
        onBack = onBack,
        onNext = onNext,
        onSkip = onSkip
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cycling Type Grid - multi-select
            Text(
                text = "Tipos de ciclismo (seleciona vários)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                items(CyclingType.entries) { type ->
                    CyclingTypeCard(
                        type = type,
                        isSelected = type in selectedTypes,
                        imageUrl = when (type) {
                            CyclingType.ROAD -> OnboardingImages.ROAD
                            CyclingType.BTT -> OnboardingImages.BTT
                            CyclingType.GRAVEL -> OnboardingImages.GRAVEL
                            CyclingType.ALL -> OnboardingImages.ALL_TYPES
                        },
                        onClick = { onToggleType(type) }
                    )
                }
            }

            // Experience Level as chips
            Text(
                text = "O teu nível",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExperienceLevel.entries.forEach { level ->
                    FilterChip(
                        selected = level == selectedLevel,
                        onClick = { onSelectLevel(level) },
                        label = {
                            Text(
                                text = level.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (level == selectedLevel) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PortugueseGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Region selector
            Text(
                text = "A tua região",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRegionDropdown = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedRegion.displayName,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Selecionar região",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = showRegionDropdown,
                    onDismissRequest = { showRegionDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Region.entries.forEach { region ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = region.displayName,
                                    fontWeight = if (region == selectedRegion) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSelectRegion(region)
                                showRegionDropdown = false
                            },
                            leadingIcon = if (region == selectedRegion) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PortugueseGreen
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CyclingTypeCard(
    type: CyclingType,
    isSelected: Boolean,
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    3.dp,
                    PortugueseGreen,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = type.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = type.emoji,
                    fontSize = 24.sp
                )
                Text(
                    text = type.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PortugueseGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalsStep(
    selected: Set<UserGoal>,
    onToggle: (UserGoal) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    OnboardingStepLayout(
        backgroundImage = OnboardingImages.GOALS,
        stepNumber = 2,
        totalSteps = 2,
        title = "O que procuras na app?",
        subtitle = "Seleciona pelo menos uma opção",
        onBack = onBack,
        onNext = onNext,
        onSkip = onSkip,
        nextEnabled = selected.isNotEmpty()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(UserGoal.entries) { goal ->
                GoalCard(
                    goal = goal,
                    isSelected = goal in selected,
                    onClick = { onToggle(goal) }
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: UserGoal,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    3.dp,
                    PortugueseGreen,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                PortugueseGreen.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = goal.emoji,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = goal.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) PortugueseGreen else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CompleteStep(
    preferences: UserPreferences,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = OnboardingImages.COMPLETE,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }
            }

            // Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Tudo pronto!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "A tua experiência está personalizada",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                // Summary card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryRow(
                            label = "Tipos",
                            value = preferences.cyclingTypes.joinToString(" ") { it.emoji }
                        )
                        SummaryRow(
                            label = "Nível",
                            value = preferences.experienceLevel.displayName
                        )
                        SummaryRow(
                            label = "Região",
                            value = preferences.favoriteRegion.displayName
                        )
                        if (preferences.goals.isNotEmpty()) {
                            SummaryRow(
                                label = "Objetivos",
                                value = preferences.goals.joinToString(", ") { it.emoji }
                            )
                        }
                    }
                }
            }

            // Finish button
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PortugueseGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Começar a explorar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun OnboardingStepLayout(
    backgroundImage: String,
    stepNumber: Int,
    totalSteps: Int,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: (() -> Unit)? = null,
    nextEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background with top image
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = backgroundImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Top bar with back, progress, and skip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }

                    // Progress indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(totalSteps) { index ->
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = if (index + 1 == stepNumber) 24.dp else 8.dp,
                                        height = 8.dp
                                    )
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (index + 1 <= stepNumber) Color.White
                                        else Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }

                    // Skip button
                    if (onSkip != null) {
                        TextButton(onClick = onSkip) {
                            Text(
                                text = "Saltar",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 160.dp)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }

            // Next button
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(52.dp),
                enabled = nextEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PortugueseGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continuar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}
