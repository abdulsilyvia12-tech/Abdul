package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlinkCyan
import com.example.ui.theme.BlinkGreen
import com.example.ui.theme.BlinkPink
import com.example.ui.theme.BlinkPrimary

@Composable
fun AvatarBadge(
    name: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showOnline: Boolean = false,
    isOnline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        BlinkPrimary
    }

    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "?" }
        .uppercase()

    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(parsedColor, parsedColor.copy(alpha = 0.7f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (showOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isOnline) BlinkGreen else Color.Gray)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
    }
}

@Composable
fun StoryRingAvatar(
    name: String,
    colorHex: String,
    hasUnseenStory: Boolean = true,
    size: Dp = 64.dp,
    onClick: () -> Unit
) {
    val ringBrush = if (hasUnseenStory) {
        Brush.sweepGradient(
            listOf(BlinkPink, BlinkPrimary, BlinkCyan, BlinkPink)
        )
    } else {
        Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.5f), Color.DarkGray))
    }

    Box(
        modifier = Modifier
            .size(size)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(ringBrush)
                .padding(2.5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .padding(2.5.dp),
            contentAlignment = Alignment.Center
        ) {
            AvatarBadge(
                name = name,
                colorHex = colorHex,
                size = size - 10.dp
            )
        }
    }
}
