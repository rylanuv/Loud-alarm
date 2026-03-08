package com.loud.alarm.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SkeuomorphicSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val xOffset by animateDpAsState(targetValue = if (checked) 26.dp else 2.dp, label = "thumbOffset")
    
    Box(
        modifier = modifier
            .width(56.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (checked) listOf(Color(0xFF8B5A2B), Color(0xFFD2691E)) // Warm skeuomorphic on
                             else listOf(Color(0xFF212121), Color(0xFF424242)) // Dark inset
                )
            )
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(15.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        // Track Inner shadow effect
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(2.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(15.dp))
        )
        
        // Thumb
        Box(
            modifier = Modifier
                .padding(start = xOffset)
                .align(Alignment.CenterStart)
                .size(26.dp)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF5F5F5), Color(0xFFBDBDBD)) // Metallic / plastic white feel
                    )
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        ) {
            // Empty thumb, grippers removed
        }
    }
}
