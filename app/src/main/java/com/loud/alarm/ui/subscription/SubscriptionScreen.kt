package com.loud.alarm.ui.subscription

import android.app.Activity
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loud.alarm.R
import com.loud.alarm.billing.BillingManager
import com.loud.alarm.billing.BillingViewModel
import com.loud.alarm.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val purchaseError by billingViewModel.purchaseError.collectAsState()

    // Collect structured offer info (includes intro pricing)
    val monthlyOfferInfo by billingViewModel.monthlyOfferInfo.collectAsState()
    val yearlyOfferInfo by billingViewModel.yearlyOfferInfo.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val activeColor = PrimaryAccent // Premium gold
    val goldGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFF3E5AB), Color(0xFFC59B27), Color(0xFFF3E5AB))
    )
    val shinyBrightGoldGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFFEA79), Color(0xFFFFD700), Color(0xFFFFEA79))
    )
    val darkBgColor = Color(0xFF151515).copy(alpha = 0.85f)
    val inactiveBorderColor = Color(0xFF333333).copy(alpha = 0.8f)
    val grayText = Color(0xFFAAB5BD)

    // --- Shimmer animation for header ---
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    val shimmerGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFC59B27),
            Color(0xFFFFF8E1),
            Color(0xFFF3E5AB),
            Color(0xFFFFF8E1),
            Color(0xFFC59B27)
        ),
        start = Offset(shimmerOffset * 800f, 0f),
        end = Offset(shimmerOffset * 800f + 600f, 200f)
    )

    // --- Staggered entrance animation states ---
    var showBadge by remember { mutableStateOf(false) }
    var showHeadline by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showTransformCard by remember { mutableStateOf(false) }
    var showFeatures by remember { mutableStateOf(false) }
    var showPlans by remember { mutableStateOf(false) }
    var showCta by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showBadge = true
        delay(100)
        showHeadline = true
        delay(100)
        showSubtitle = true
        delay(150)
        showTransformCard = true
        delay(150)
        showFeatures = true
        delay(150)
        showPlans = true
        delay(150)
        showCta = true
    }

    LaunchedEffect(isSubscribed) {
        if (isSubscribed) {
            onBack()
        }
    }

    // Retry loading subscription details when screen opens
    LaunchedEffect(Unit) {
        billingViewModel.retryLoadingDetails()
    }

    // Show error Snackbar when purchaseError changes
    LaunchedEffect(purchaseError) {
        purchaseError?.let { error ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Short
                )
                billingViewModel.clearPurchaseError()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.newsubsc),
            contentDescription = "Subscription Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF2C2C2C),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
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

                // ===== 1. PREMIUM BADGE (animated entrance) =====
                AnimatedVisibility(
                    visible = showBadge,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -40 }
                ) {
                    Box(
                        modifier = Modifier
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===== 2. ANIMATED SHIMMER HEADLINE =====
                AnimatedVisibility(
                    visible = showHeadline,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -30 }
                ) {
                    Text(
                        text = "Never Sleep In\nAgain",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 44.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .graphicsLayer(alpha = 0.99f)
                            .drawWithCache {
                                onDrawWithContent {
                                    drawContent()
                                    drawRect(shimmerGradient, blendMode = BlendMode.SrcAtop)
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ===== 3. SUBTITLE =====
                AnimatedVisibility(
                    visible = showSubtitle,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 20 }
                ) {
                    Text(
                        text = "Join the heavy sleepers who finally took control of their mornings.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ===== 4. BEFORE / AFTER TRANSFORMATION CARD =====
                AnimatedVisibility(
                    visible = showTransformCard,
                    enter = fadeIn(tween(600)) + expandVertically(tween(600))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    ) {
                        Column {
                            // "WITHOUT PRO" row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFF453A).copy(alpha = 0.08f))
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFF453A).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AlarmOff,
                                        contentDescription = null,
                                        tint = Color(0xFFFF453A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        "Without Pro",
                                        color = Color(0xFFFF453A).copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "No follow-up checks; risk falling back asleep",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 20.sp
                                    )
                                }
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.06f))
                            )

                            // "WITH PRO" row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.08f))
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        "With Pro",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Wake-Up Check & 15+ challenges guarantee wakefulness",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ===== 5. ENHANCED FEATURE SHOWCASE (3 items) =====
                AnimatedVisibility(
                    visible = showFeatures,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 40 }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PremiumBenefitItem(
                            icon = Icons.Default.Extension,
                            iconBgColor = Color(0xFF7C4DFF),
                            iconTint = Color(0xFFD1C4E9),
                            title = "15+ Mental and Physical Challenges",
                            subtitle = "Math, Maze, Shake, Steps, Scan & more force you fully awake."
                        )
                        PremiumBenefitItem(
                            icon = Icons.Default.CheckCircle,
                            iconBgColor = Color(0xFF4CAF50),
                            iconTint = Color(0xFFC8E6C9),
                            title = "Wake-Up Verification",
                            subtitle = "A follow-up check ensures you didn't crawl back to bed."
                        )
                        PremiumBenefitItem(
                            icon = Icons.Default.Vibration,
                            iconBgColor = Color(0xFFFF6B35),
                            iconTint = Color(0xFFFFCCBC),
                            title = "Power Vibrations",
                            subtitle = "5 haptic patterns that shake you out of deep sleep."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // ===== 6. PLAN SELECTION =====
                AnimatedVisibility(
                    visible = showPlans,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 50 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "CHOOSE YOUR PLAN",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))

                        // --- Annual Plan (Default selected, BEST VALUE) ---
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            val isSelected = selectedPlan == 1
                            val borderColor = if (isSelected) activeColor else inactiveBorderColor
                            val yearlyInfo = yearlyOfferInfo

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) activeColor.copy(alpha = 0.1f) else darkBgColor)
                                    .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                                    .clickable { selectedPlan = 1 }
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CustomRadioButton(selected = isSelected, color = activeColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Annual", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    if (yearlyInfo?.hasIntroOffer == true) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                yearlyInfo.discountPercentage?.let { "$it% OFF" } ?: "50% OFF",
                                                color = Color(0xFF4CAF50),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.5.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Best Value",
                                            style = if (isSelected) {
                                                androidx.compose.ui.text.TextStyle(brush = shinyBrightGoldGradient)
                                            } else {
                                                androidx.compose.ui.text.TextStyle(color = grayText)
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    if (yearlyInfo?.hasIntroOffer == true) {
                                        Text(
                                            text = yearlyInfo.introPrice ?: "",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = yearlyInfo.regularPrice ?: "",
                                                color = grayText,
                                                fontSize = 12.sp,
                                                textDecoration = TextDecoration.LineThrough,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = " ${yearlyInfo.regularPeriodDesc ?: "per year"}",
                                                color = grayText,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    } else {
                                        Text(yearlyPrice ?: "${'$'}12.99", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("per year", color = grayText, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // --- Monthly Plan ---
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            val isSelected = selectedPlan == 2
                            val borderColor = if (isSelected) activeColor else inactiveBorderColor
                            val monthlyInfo = monthlyOfferInfo

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) activeColor.copy(alpha = 0.1f) else darkBgColor)
                                    .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                                    .clickable { selectedPlan = 2 }
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CustomRadioButton(selected = isSelected, color = activeColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Monthly", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    if (monthlyInfo?.hasIntroOffer == true) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            val discountText = monthlyInfo.discountPercentage?.let { "$it% OFF" } ?: "50% OFF"
                                            val periodText = monthlyInfo.introPeriodDesc?.removePrefix("for ")?.uppercase() ?: "3 MONTHS"
                                            Text(
                                                "$discountText · $periodText",
                                                color = Color(0xFF4CAF50),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.5.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    } else {
                                        Text("Try It Out", color = grayText, fontSize = 14.sp, maxLines = 1)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    if (monthlyInfo?.hasIntroOffer == true) {
                                        Text(
                                            text = monthlyInfo.introPrice ?: "",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = monthlyInfo.regularPrice ?: "",
                                                color = grayText,
                                                fontSize = 12.sp,
                                                textDecoration = TextDecoration.LineThrough,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = " ${monthlyInfo.regularPeriodDesc ?: "per month"}",
                                                color = grayText,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    } else {
                                        Text(monthlyPrice ?: "${'$'}1.49", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("per month", color = grayText, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // --- Lifetime Plan ---
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                            val isSelected = selectedPlan == 0
                            val borderColor = if (isSelected) activeColor else inactiveBorderColor

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) activeColor.copy(alpha = 0.1f) else darkBgColor)
                                    .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                                    .clickable { selectedPlan = 0 }
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CustomRadioButton(selected = isSelected, color = activeColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Lifetime", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text("Yours forever", color = grayText, fontSize = 14.sp, maxLines = 1)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(lifetimePrice ?: "$20.00", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("one-time", color = grayText, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // ===== 7. CTA BUTTON WITH GLOW =====
                AnimatedVisibility(
                    visible = showCta,
                    enter = fadeIn(tween(700)) + scaleIn(tween(700), initialScale = 0.9f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.03f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = FastOutSlowInEasing),
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

                        // ===== 8. ENHANCED TRUST FOOTER =====
                        FlowRow(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Secure checkout", color = Color(0xFF6B7280), fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("·", color = Color(0xFF6B7280), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                                Icon(Icons.Default.EventRepeat, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel anytime", color = Color(0xFF6B7280), fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("·", color = Color(0xFF6B7280), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("No hidden fees", color = Color(0xFF6B7280), fontSize = 11.sp)
                            }
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
    }
}

// --- Helper: Calculate savings text ---
private fun calculateSavingsText(monthlyPrice: String?, yearlyPrice: String?): String {
    try {
        val monthly = monthlyPrice?.replace(Regex("[^\\d.]"), "")?.toDoubleOrNull() ?: return "Best value"
        val yearly = yearlyPrice?.replace(Regex("[^\\d.]"), "")?.toDoubleOrNull() ?: return "Best value"
        val annualizedMonthly = monthly * 12
        if (annualizedMonthly <= 0) return "Best value"
        val savings = ((annualizedMonthly - yearly) / annualizedMonthly * 100).toInt()
        return if (savings > 0) "Save $savings%" else "Best value"
    } catch (_: Exception) {
        return "Best value"
    }
}

@Composable
private fun CustomRadioButton(selected: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) color else Color.Gray.copy(alpha = 0.5f), CircleShape),
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
private fun PremiumBenefitItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBgColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
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
