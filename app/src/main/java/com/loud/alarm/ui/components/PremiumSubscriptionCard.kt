package com.loud.alarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loud.alarm.ui.theme.PrimaryAccent

@Composable
fun PremiumSubscriptionCard(
    isPremium: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Refined, more muted gold palette (less "yellow", more "premium metal")
    val goldGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFD4AF37), // Metallic Gold
            Color(0xFFCCAC5D), // Polished Brass
            Color(0xFFB59345), // Old Gold
            Color(0xFFCCAC5D), // Polished Brass
            Color(0xFFD4AF37)  // Metallic Gold
        )
    )
    val mutedGold = Color(0xFFCCAC5D)
    val mutedGoldText = Color(0xFFD4AF37)

    val cardBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1C1310), // Very dark warm brown
            Color(0xFF0F0B09), // Almost black
            Color(0xFF1C1310)  // Deep brown
        )
    )

    // Skeuomorphic button style
    val buttonGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFD4B97C),
            Color(0xFFB59345)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFFFFD700).copy(alpha = 0.2f),
                spotColor = Color(0xFFFFD700).copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackgroundGradient)
            .border(
                width = if (isPremium) 1.5.dp else 1.2.dp,
                brush = if (isPremium) {
                    Brush.sweepGradient(
                        colors = listOf(
                            mutedGold,
                            mutedGold.copy(alpha = 0.2f),
                            mutedGold,
                            mutedGold.copy(alpha = 0.2f),
                            mutedGold
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            mutedGold.copy(alpha = 0.8f),
                            mutedGold.copy(alpha = 0.1f),
                            mutedGold.copy(alpha = 0.8f)
                        )
                    )
                },
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(enabled = !isPremium, onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon Container - Jewel Look
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFE082),
                                Color(0xFFC4812A)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner Glow/Shadow for the icon box
                if (isPremium) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(2.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                )
                
                Icon(
                    imageVector = if (isPremium) Icons.Default.Star else Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(18.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) "PRO Member Status" else "Get Premium Access",
                    color = mutedGoldText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isPremium) "Enjoying unlimited access to all features" else "Unlock all challenges & features",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isPremium) {
                Spacer(Modifier.width(10.dp))

                // Action Button - Tactile Look (Only show for non-premium)
                Box(
                    modifier = Modifier
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(99.dp))
                        .clip(RoundedCornerShape(99.dp))
                        .background(buttonGradient)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(99.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Join",
                            color = Color(0xFF2C1904), // Dark brown for contrast
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = ">",
                            color = Color(0xFF2C1904),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            } else {
                // For Premium, show a subtle check icon instead of a "Manage" button
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = mutedGoldText,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
