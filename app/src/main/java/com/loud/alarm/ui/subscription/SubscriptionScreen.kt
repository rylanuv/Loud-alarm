package com.loud.alarm.ui.subscription

import android.app.Activity
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    val activeColor = PrimaryAccent // Replacing cyan with the app's native premium gold
    val darkBgColor = Color(0xFF132331).copy(alpha = 0.5f)
    val inactiveBorderColor = Color(0xFF425665).copy(alpha = 0.6f)
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
        // Dark overlay matching app's amoled/dark aesthetics
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
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
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Unlock PRO",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Get all premium tools to guarantee you never sleep in.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                // Hero Feature: Wake Up Check with Offset Design
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .offset(x = (-10).dp) // Offset for dynamic look
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle, 
                                contentDescription = null, 
                                tint = Color(0xFF4CAF50), // Changed to Green
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                "Wake Up Check", 
                                color = activeColor, 
                                fontSize = 22.sp, 
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "The ultimate safeguard to ensure you're truly awake after the alarm stops.", 
                                color = Color.White.copy(alpha = 0.8f), 
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Unified Value Section
                Text("WHAT YOU GET", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BenefitItem(
                        icon = Icons.Default.CheckCircle, 
                        title = "Wake-up verification", 
                        subtitle = "Proactive checks to verify alertness."
                    )
                    BenefitItem(
                        icon = Icons.Default.Extension, 
                        title = "Advanced challenges", 
                        subtitle = "Full access to our entire challenge library."
                    )
                    BenefitItem(
                        icon = Icons.Default.Vibration, 
                        title = "Custom vibrations", 
                        subtitle = "Exclusive high-intensity vibration patterns."
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Grouped Challenges Section
                // Brain Challenges
                Text("BRAIN CHALLENGES", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        UpgradeFeatureRowCompact(Icons.Default.Psychology, "Memory", IconPink, Modifier.weight(1f))
                        UpgradeFeatureRowCompact(Icons.Default.Extension, "Puzzle", IconIndigo, Modifier.weight(1f))
                    }
                    UpgradeFeatureRowCompact(Icons.Default.Spellcheck, "Spell Bee", IconAmber, Modifier.fillMaxWidth(0.5f))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Physical Challenges
                Text("PHYSICAL CHALLENGES", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UpgradeFeatureRowCompact(Icons.AutoMirrored.Filled.DirectionsWalk, "Steps", IconOrange, Modifier.weight(1f))
                    UpgradeFeatureRowCompact(Icons.Default.Vibration, "Shake", IconCyan, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Hardcore Challenges
                Text("HARDCORE CHALLENGES", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UpgradeFeatureRowCompact(Icons.Default.Gamepad, "Maze", IconGreen, Modifier.weight(1f))
                    UpgradeFeatureRowCompact(Icons.Default.Edit, "Rewrite", IconYellow, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(64.dp))

                // Pricing Cards
                // Lifetime Plan
                Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    val isSelected = selectedPlan == 0
                    val borderColor = if (isSelected) activeColor else inactiveBorderColor

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(darkBgColor)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { selectedPlan = 0 }
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomRadioButton(selected = isSelected, color = activeColor)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lifetime", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text("One-time payment", color = grayText, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(lifetimePrice ?: "$20.00", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Pay once, use forever", color = activeColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Badge
                    Box(
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .background(activeColor, RoundedCornerShape(percent = 50))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("BEST VALUE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Annual Plan
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    val isSelected = selectedPlan == 1
                    val borderColor = if (isSelected) activeColor else inactiveBorderColor

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(darkBgColor)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { selectedPlan = 1 }
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomRadioButton(selected = isSelected, color = activeColor)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Annual", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text("Best monthly value", color = grayText, fontSize = 14.sp)
                        }
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                                    append(yearlyPrice ?: "$12.99")
                                }
                                withStyle(style = SpanStyle(color = grayText, fontSize = 14.sp)) {
                                    append("/year")
                                }
                            }
                        )
                    }
                }

                // Monthly Plan
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    val isSelected = selectedPlan == 2
                    val borderColor = if (isSelected) activeColor else inactiveBorderColor

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(darkBgColor)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { selectedPlan = 2 }
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomRadioButton(selected = isSelected, color = activeColor)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Monthly", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text("Cancel anytime", color = grayText, fontSize = 14.sp)
                        }
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                                    append(monthlyPrice ?: "$1.49")
                                }
                                withStyle(style = SpanStyle(color = grayText, fontSize = 14.sp)) {
                                    append("/month")
                                }
                            }
                        )
                    }
                }

                // Action Button - Glassmorphism
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, activeColor.copy(alpha = 0.6f), RoundedCornerShape(percent = 50))
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
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = activeColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val btnText = when (selectedPlan) {
                            0 -> "Start Lifetime Plan"
                            1 -> "Start Annual Plan"
                            else -> "Start Monthly Plan"
                        }
                        Text(
                            text = btnText,
                            color = activeColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Restore Purchase",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                         billingViewModel.restorePurchases()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "By subscribing, you agree to our Terms of Service and Privacy Policy. Subscription automatically renews unless cancelled at least 24 hours before the end of the current period.",
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
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
            .border(2.dp, if (selected) color else Color.Gray, CircleShape),
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
private fun UpgradeFeatureRowCompact(icon: ImageVector, text: String, iconColor: Color, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BenefitItem(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryAccent,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

