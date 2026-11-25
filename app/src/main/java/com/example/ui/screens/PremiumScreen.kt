package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.billing.PlayBillingState
import com.example.billing.RazorpayUiState
import com.example.model.PaymentTransaction
import com.example.model.PlanConfig
import com.example.model.SubscriptionRecord
import com.example.model.SubscriptionStatus
import com.example.model.UiState
import com.example.model.UserEntitlement
import com.example.ui.theme.CoralRose
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PremiumScreen(
    planConfigState: UiState<PlanConfig>,
    entitlementState: UiState<UserEntitlement>,
    subscriptionsState: UiState<List<SubscriptionRecord>>,
    transactionsState: UiState<List<PaymentTransaction>>,
    playBillingState: PlayBillingState,
    razorpayState: RazorpayUiState,
    canUseRazorpay: Boolean,
    isBackendConfigured: Boolean,
    billingBusy: Boolean,
    onBack: () -> Unit,
    onPurchasePlay: (billingInterval: String) -> Unit,
    onPurchaseRazorpay: (billingInterval: String) -> Unit,
    onRestorePurchases: () -> Unit,
    onCancelSubscription: (subscriptionId: String, reason: String?) -> Unit,
    onDismissBillingMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    val planConfig = (planConfigState as? UiState.Success)?.data ?: PlanConfig()
    val entitlement = (entitlementState as? UiState.Success)?.data ?: UserEntitlement()
    val subscriptions = (subscriptionsState as? UiState.Success)?.data.orEmpty()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()

    var selectedInterval by remember { mutableStateOf("yearly") }
    var cancelTargetId by remember { mutableStateOf<String?>(null) }
    var cancelReason by remember { mutableStateOf("") }

    if (cancelTargetId != null) {
        AlertDialog(
            onDismissRequest = { cancelTargetId = null },
            title = { Text("Cancel Subscription?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You will keep full TejasCode Pro access until the end of your current paid billing cycle.")
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { if (it.length <= 160) cancelReason = it },
                        label = { Text("Optional reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        cancelTargetId?.let { onCancelSubscription(it, cancelReason.ifBlank { null }) }
                        cancelTargetId = null
                        cancelReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTargetId = null }) { Text("Keep Pro") }
            }
        )
    }

    val monthlyPriceText = playBillingState.monthlyFormattedPrice ?: planConfig.formattedMonthlyPrice()
    val yearlyPriceText = playBillingState.yearlyFormattedPrice ?: planConfig.formattedYearlyPrice()
    val canPurchase = planConfig.isConfigured && isBackendConfigured && !playBillingState.isBusy && !razorpayState.isBusy && !billingBusy

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("premium_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("TejasCode Pro", style = MaterialTheme.typography.titleLarge)
                        Text("Subscriptions & Billing", style = MaterialTheme.typography.bodySmall, color = NeonBlue)
                    }
                }
                TextButton(
                    onClick = onRestorePurchases,
                    enabled = !playBillingState.isBusy,
                    modifier = Modifier.testTag("restore_purchases_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restore purchases", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restore")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Billing Status Messages
            val statusMsg = playBillingState.statusMessage ?: razorpayState.statusMessage
            val isErr = if (playBillingState.statusMessage != null) playBillingState.isError else razorpayState.isError
            if (statusMsg != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isErr) MaterialTheme.colorScheme.errorContainer else NeonGreen.copy(alpha = 0.14f)
                        ),
                        border = BorderStroke(1.dp, if (isErr) CoralRose else NeonGreen),
                        onClick = onDismissBillingMessages
                    ) {
                        Text(
                            text = "$statusMsg (Tap to dismiss)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isErr) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Verified Entitlement Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, if (entitlement.isProActive) NeonGreen else NeonPurple.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    if (entitlement.isProActive) listOf(Color(0xFF063B2B), Color(0xFF0A244D))
                                    else listOf(Color(0xFF191247), Color(0xFF0B1438))
                                )
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Membership Tier",
                                tint = if (entitlement.isProActive) NeonGreen else NeonAmber,
                                modifier = Modifier.size(30.dp)
                            )
                            Column {
                                Text(
                                    text = if (entitlement.isProActive) "TejasCode Pro Active" else "Free Starter Plan",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Status: ${entitlement.subscriptionStatus.label} • Provider: ${entitlement.provider}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonBlue
                                )
                            }
                        }
                        val endMs = entitlement.periodEndEpochMs
                        if (endMs != null && endMs > 0L) {
                            val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(endMs))
                            Text(
                                text = if (entitlement.cancelAtPeriodEnd) "Access expires on $dateFmt" else "Next renewal: $dateFmt",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Plan Selector & Comparison
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Compare Free vs TejasCode Pro", style = MaterialTheme.typography.titleMedium)

                        listOf(
                            "All 15 Programming Languages (Python, Java, JS, C, C++, C#, PHP, HTML, CSS, SQL, Kotlin, Swift, Go, Rust, TypeScript)",
                            "Unlock Advanced & Pro Interview Tracks",
                            "${planConfig.proDailyAiLimit} Daily Gemini AI Tutor & Sandbox Compiles (vs ${planConfig.freeDailyAiLimit} Free)",
                            "Verified QR + PDF Certificates & Cloud Workspace"
                        ).forEach { benefit ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                Text(benefit, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = selectedInterval == "monthly",
                                onClick = { selectedInterval = "monthly" },
                                label = { Text("Monthly • $monthlyPriceText") },
                                modifier = Modifier.weight(1f).testTag("plan_chip_monthly")
                            )
                            FilterChip(
                                selected = selectedInterval == "yearly",
                                onClick = { selectedInterval = "yearly" },
                                label = { Text("Yearly • $yearlyPriceText") },
                                modifier = Modifier.weight(1f).testTag("plan_chip_yearly")
                            )
                        }

                        if (!planConfig.isConfigured) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = NeonAmber.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Pricing Pending: The administrator must configure Monthly & Yearly plan amounts in Admin -> Pricing & Plans before checkouts can start.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else if (!isBackendConfigured) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = "Server verification endpoint (TEJASCODE_BACKEND_URL) is not configured in AI Studio Secrets. Pro activation requires live server-side verification.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (planConfig.trialDays > 0) {
                            Text(
                                text = "Includes ${planConfig.trialDays}-day free trial for eligible subscribers.",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonGreen
                            )
                        }

                        Text(
                            text = planConfig.billingTermsSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { onPurchasePlay(selectedInterval) },
                            enabled = canPurchase,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("subscribe_play_button")
                        ) {
                            if (playBillingState.isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Subscribe via Google Play (${if (selectedInterval == "yearly") yearlyPriceText else monthlyPriceText})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (canUseRazorpay) {
                            OutlinedButton(
                                onClick = { onPurchaseRazorpay(selectedInterval) },
                                enabled = canPurchase,
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("subscribe_razorpay_button")
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Pay via Razorpay (UPI AutoPay / Card / NetBanking)")
                            }
                        } else if (planConfig.razorpayEnabled) {
                            Text(
                                text = "Razorpay UPI/Card billing is enabled on your server and unlocks in-app once PLAY_ALTERNATIVE_BILLING_APPROVED=true is configured.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Active / Past Subscriptions
            item {
                Text("Your Subscriptions", style = MaterialTheme.typography.titleMedium)
            }

            if (subscriptions.isEmpty()) {
                item {
                    Text(
                        text = "No verified subscription records yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(subscriptions, key = { it.id }) { sub ->
                    val endFmt = if (sub.currentPeriodEndMs > 0L) {
                        SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(sub.currentPeriodEndMs))
                    } else "—"
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${sub.billingInterval.replaceFirstChar { it.uppercase() }} • ${sub.provider}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = NeonBlue.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = sub.subscriptionStatus.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Amount: ${PlanConfig.formatMinorAmount(sub.amountMinorUnits, planConfig.currencySymbol)} • Period Ends: $endFmt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val canCancel = (sub.subscriptionStatus == SubscriptionStatus.Active ||
                                sub.subscriptionStatus == SubscriptionStatus.Trialing) && !sub.cancelAtPeriodEnd
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (canCancel) {
                                    OutlinedButton(
                                        onClick = { cancelTargetId = sub.subscriptionId.ifBlank { sub.id } },
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Cancel Renewal", color = CoralRose)
                                    }
                                }
                                if (sub.provider == "google_play") {
                                    TextButton(
                                        onClick = {
                                            val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        },
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Manage in Play Store")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Payment Transactions & Invoices
            item {
                Text("Payment & Invoice History", style = MaterialTheme.typography.titleMedium)
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "No payment invoices recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    val dateStr = if (tx.createdAtMs > 0L) {
                        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date(tx.createdAtMs))
                    } else "—"
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = NeonAmber)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${tx.invoiceRef.ifBlank { tx.transactionId }} • ${tx.status.uppercase()}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "$dateStr • ${tx.provider} (${tx.paymentMethodSummary.ifBlank { tx.billingInterval }})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = PlanConfig.formatMinorAmount(tx.grossAmountMinor, planConfig.currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
