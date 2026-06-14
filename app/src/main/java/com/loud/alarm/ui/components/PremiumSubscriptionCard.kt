package com.loud.alarm.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
    // Always vibrant, never dull. No more dark brown edges!
    val vibrantGold = Color(0xFFFFD700) // Extremely bright gold
    val deepGold = Color(0xFFD4AF37) // Rich metallic gold for depth
    val whiteGlare = Color(0xFFFFFFFF) // Pure white-hot glare
    
    val themeGradientColors = listOf(
        Color.Transparent, // Transparent background so the solid gold underneath shows
        Color.Transparent,
        deepGold.copy(alpha = 0.2f),   // Soft gold glow
        whiteGlare.copy(alpha = 0.4f), // The sharp 40% opacity white reflection
        deepGold.copy(alpha = 0.2f),   // Soft gold glow
        Color.Transparent,
        Color.Transparent
    )

    // A mathematically seamless, infinite continuous flow of light
    val infiniteTransition = rememberInfiniteTransition(label = "theme_shimmer")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2400f, // Exactly 2x the gradient width for a seamless loop
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing), // Slower animation (8s loop)
            repeatMode = RepeatMode.Restart // Seamless restart
        ),
        label = "shimmer_offset"
    )

    val animatedBrush = Brush.linearGradient(
        colors = themeGradientColors,
        start = androidx.compose.ui.geometry.Offset(offset, offset),
        end = androidx.compose.ui.geometry.Offset(offset + 1200f, offset + 1200f),
        tileMode = androidx.compose.ui.graphics.TileMode.Mirror // Mirrors the gradient continuously
    )

    val staticGoldBrush = Brush.linearGradient(
        colors = listOf(vibrantGold, deepGold, vibrantGold)
    )

    val cardBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF121212), // App's SurfaceVariantDark
            Color(0xFF0A0A0A)  // App's SurfaceDark
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp, // High elevation
                shape = RoundedCornerShape(16.dp),
                ambientColor = vibrantGold.copy(alpha = 0.25f),
                spotColor = vibrantGold.copy(alpha = 0.5f) // Strong gold glow
            )
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .border(
                width = 1.5.dp,
                brush = staticGoldBrush, // Static premium gold border
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isPremium, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 32.dp) // Further increased vertical padding to make it taller
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top row: Icon + Texts
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sleek, glowing icon container that stands out heavily
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isPremium) staticGoldBrush else SolidColor(vibrantGold.copy(alpha = 0.15f)))
                        .border(1.5.dp, if (isPremium) SolidColor(Color.Transparent) else staticGoldBrush, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPremium) Icons.Default.Star else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isPremium) Color(0xFF141414) else vibrantGold, // Brilliant gold lock
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPremium) "PRO Member" else "Solve2Wake PRO",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        style = if (isPremium) androidx.compose.ui.text.TextStyle(brush = staticGoldBrush) else androidx.compose.ui.text.TextStyle.Default
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (isPremium) "Unlimited access to all features" else "Unlock all premium challenges",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }

                if (isPremium) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = vibrantGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // CTA Button for non-premium users
            if (!isPremium) {
                Spacer(Modifier.height(26.dp)) // Further increased spacing before button to add height

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(10.dp), spotColor = vibrantGold.copy(alpha = 0.4f)) // Golden shadow for button
                        .clip(RoundedCornerShape(10.dp))
                        .background(staticGoldBrush) // Solid base gold
                        .background(animatedBrush) // Subtle strobe sweep overlay (40% opacity)
                        .padding(vertical = 10.dp), // Still slim
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Upgrade Now",
                            color = Color(0xFF000000), // Pure black text for high contrast
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF000000),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

