package com.ciclismo.portugal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.User

/**
 * A reusable profile icon button that displays:
 * - User's profile photo if available
 * - User's initials if logged in but no photo
 * - Default AccountCircle icon if not logged in
 */
@Composable
fun ProfileIconButton(
    user: User?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        when {
            // User is logged in and has a photo
            user != null && !user.photoUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Perfil",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            // User is logged in but no photo - show initials
            user != null -> {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF006600)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getInitials(user.displayName),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            // Not logged in - show default icon
            else -> {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Perfil",
                    tint = tint
                )
            }
        }
    }
}

/**
 * Extract initials from a display name.
 * Examples:
 * - "John Doe" -> "JD"
 * - "John" -> "J"
 * - "" -> "?"
 */
private fun getInitials(displayName: String): String {
    val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
    }
}
