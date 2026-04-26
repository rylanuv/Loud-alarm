package com.loud.alarm.ui.subscription

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loud.alarm.R
import com.loud.alarm.billing.BillingManager
import com.loud.alarm.billing.BillingViewModel
import com.loud.alarm.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    billingViewModel: BillingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val isSubscribed by billingViewModel.isSubscribed.collectAsState()
    var selectedPlan by remember { mutableStateOf(1) } // 0 = Lifetime, 1 = Annual, 2 = Monthly

    // Collect real prices from billing
    val lifetimePrice by billingViewModel.lifetimePrice.collectAsState()
    val monthlyPrice by billingViewModel.monthlyPrice.collectAsState()
    val yearlyPrice by billingViewModel.yearlyPrice.collectAsState()

    val activeColor = PrimaryAccent // Premium gold
    val goldGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFF3E5AB), Color(0xFFC59B27), Color(0xFFF3E5AB))
    )
    val darkBgColor = Color(0xFF151515).copy(alpha = 0.85f)
    val inactiveBorderColor = Color(0xFF333333).copy(alpha = 0.8f)
    val grayText = Color(0xFFAAB5BD)

    LaunchedEffect(isSubscribed) {
        if (isSubscribed) {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.subscription),
            contentDescription = "Subscription Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Darker overlay to make premium gold pop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=0.4f), Color.Black.copy(alpha=0.9f), Color.Black)))
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Premium Badge
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, activeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = activeColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SOLVE2WAKE PRO", color = activeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Never Sleep In\nAgain.",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer(alpha = 0.99f).drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(goldGradient, blendMode = BlendMode.SrcAtop)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Join thousands of heavy sleepers who finally took control of their mornings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hero Feature: Wake Up Check with Offset Design
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, activeColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle, 
                                contentDescription = null, 
                                tint = Color(0xFF4CAF50), 
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                "Wake Up Check", 
                                color = activeColor, 
                                fontSize = 20.sp, 
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "The ultimate safeguard to ensure you're truly awake after the alarm stops.", 
                                color = Color.White.copy(alpha = 0.9f), 
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Standard Benefits
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BenefitItem(
                        icon = Icons.Default.Extension, 
                        title = "Full Challenge Library", 
                        subtitle = "Memory, Puzzle, Translate, Maze & more."
                    )
                    BenefitItem(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk, 
                        title = "Physical Challenges", 
                        subtitle = "Force yourself out of bed with Step & Shake tasks."
                    )
                    BenefitItem(
                        icon = Icons.AutoMirrored.Filled.VolumeUp, 
                        title = "Aggressive Mode", 
                        subtitle = "Loudest volumes & unignorable vibration patterns."
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text("CHOOSE YOUR PLAN", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))

                // Annual Plan (Default selected, BEST VALUE)
                Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    val isSelected = selectedPlan == 1
                    val borderColor = if (isSelected) activeColor else inactiveBorderColor

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) activeColor.copy(alpha=0.1f) else darkBgColor)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { selectedPlan = 1 }
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomRadioButton(selected = isSelected, color = activeColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Annual", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("Worth It", color = if(isSelected) activeColor else grayText, fontSize = 14.sp, maxLines = 1, fontWeight = if(isSelected) FontWeight.Medium else FontWeight.Normal)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(yearlyPrice ?: "$12.99", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("per year", color = grayText, fontSize = 12.sp, maxLines = 1)
                        }
                    }

                    // Badge
                    Box(
                        modifier = Modifier
                            .padding(end = 20.dp)
                            .background(goldGradient, RoundedCornerShape(percent = 50))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("BEST VALUE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    }
                }

                // Monthly Plan
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    val isSelected = selectedPlan == 2
                    val borderColor = if (isSelected) activeColor else inactiveBorderColor

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) activeColor.copy(alpha=0.1f) else darkBgColor)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { selectedPlan = 2 }
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomRadioButton(selected = isSelected, color = activeColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Monthly", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("Try It Out", color = grayText, fontSize = 14.sp, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(monthlyPrice ?: "$1.49", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("per month", color = grayText, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }

                // Lifetime Plan
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    val isSelected = selectedPlan == 0
                    val borderColor = if (isSelected) activeColor else inactiveBorderColor

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) activeColor.copy(alpha=0.1f) else darkBgColor)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { selectedPlan = 0 }
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomRadioButton(selected = isSelected, color = activeColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lifetime", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("Pay once, yours forever", color = grayText, fontSize = 14.sp, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(lifetimePrice ?: "$20.00", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("one-time", color = grayText, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }

                // Action Button - Animated
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(percent = 50))
                        .background(goldGradient)
                        .clickable {
                            val planType = when (selectedPlan) {
                                0 -> BillingManager.PRODUCT_ID_LIFETIME
                                1 -> BillingManager.PRODUCT_ID_YEARLY
                                else -> BillingManager.PRODUCT_ID_MONTHLY
                            }
                            billingViewModel.purchaseSubscription(activity, planType)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "UNLOCK PRO NOW",
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Secure / Guarantee subtext
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel anytime. No hidden fees.", color = Color.Gray, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Restore Purchase",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                         billingViewModel.restorePurchases()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "By subscribing, you agree to our Terms of Service and Privacy Policy.\nSubscription automatically renews unless cancelled at least 24 hours before the end of the current period.",
                    color = Color.Gray.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun CustomRadioButton(selected: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) color else Color.Gray.copy(alpha=0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun BenefitItem(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryAccent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}


