package com.loud.alarm.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
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
    // Elegant gold gradient for premium feel
    val goldGradient = listOf(
        Color(0xFFFFDF73),
        Color(0xFFD4AF37),
        Color(0xFF996515),
        Color(0xFFD4AF37)
    )
    
    val premiumBgTop = Color(0xFF1F1A12).copy(alpha = 0.6f) 
    val premiumBgBottom = Color(0xFF0F0C08).copy(alpha = 0.75f)
    val defaultBgTop = Color(0xFF1A1A1A).copy(alpha = 0.6f)
    val defaultBgBottom = Color(0xFF0A0A0A).copy(alpha = 0.75f)
    
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = !isPremium, onClick = onClick)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPremium) {
                        listOf(premiumBgTop, premiumBgBottom)
                    } else {
                        listOf(defaultBgTop, defaultBgBottom)
                    }
                )
            )
            .drawWithCache {
                val borderBrush = if (!isPremium) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x33FFDF73),
                            Color(0xFFFFDF73),
                            Color(0x33FFDF73)
                        ),
                        start = Offset(shimmerOffset, shimmerOffset),
                        end = Offset(shimmerOffset + 500f, shimmerOffset + 500f)
                    )
                } else {
                    Brush.linearGradient(
                        colors = goldGradient,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                }
                
                onDrawBehind {
                    // Draw a solid base stroke throughout for non-premium
                    if (!isPremium) {
                        drawRoundRect(
                            color = Color(0xFFFFDF73).copy(alpha = 0.3f),
                            cornerRadius = CornerRadius(24.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    
                    drawRoundRect(
                        brush = borderBrush,
                        cornerRadius = CornerRadius(24.dp.toPx()),
                        style = Stroke(width = if (isPremium) 1.5.dp.toPx() else 2.dp.toPx())
                    )
                }
            }
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon Container
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPremium) Brush.linearGradient(goldGradient) 
                            else Brush.linearGradient(listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A)))
                        )
                        .border(
                            width = 1.dp,
                            brush = if (!isPremium) Brush.linearGradient(goldGradient) else SolidColor(Color.Transparent),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPremium) Icons.Default.WorkspacePremium else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isPremium) Color.Black else Color(0xFFFFCA28),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPremium) "PRO Member" else "Solve2Wake PRO",
                        color = if (isPremium) Color(0xFFFFDF73) else Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isPremium) "Unlimited access to all features" else "Unlock all premium challenges",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!isPremium) {
                Spacer(Modifier.height(28.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE5B73B))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Upgrade Now",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFDF73).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFFDF73), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = Color(0xFFFFDF73),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "You're All Set",
                        color = Color(0xFFFFDF73),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

