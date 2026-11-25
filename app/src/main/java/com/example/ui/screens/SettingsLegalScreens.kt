package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferencesManager
import com.example.model.UserProfile
import com.example.ui.theme.CoralRose
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.util.LocalAppLabels

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsLegalScreen(
    profile: UserProfile?,
    userEmail: String?,
    darkThemeEnabled: Boolean,
    selectedLanguageCode: String,
    rememberMeEnabled: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onToggleRememberMe: (Boolean) -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val labels = LocalAppLabels.current

    var legalDialogTitle by remember { mutableStateOf<String?>(null) }
    var legalDialogBody by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteConfirmInput by remember { mutableStateOf("") }

    if (legalDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { legalDialogTitle = null },
            title = { Text(legalDialogTitle.orEmpty()) },
            text = { Text(legalDialogBody, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                Button(onClick = { legalDialogTitle = null }) { Text("Close") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Permanently?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This removes your TejasCode profile, saved code snippets, certificates, and revokes access. Active paid subscriptions should also be cancelled in Google Play Store.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = deleteConfirmInput,
                        onValueChange = { deleteConfirmInput = it },
                        label = { Text("Type DELETE to confirm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("delete_confirm_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        deleteConfirmInput = ""
                        onDeleteAccount()
                    },
                    enabled = deleteConfirmInput.trim().equals("DELETE", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(labels.settingsTitle, style = MaterialTheme.typography.titleLarge)
                    Text("Theme, Language, Security & Legal", style = MaterialTheme.typography.bodySmall, color = NeonBlue)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme & Language
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = NeonBlue)
                                Column {
                                    Text(labels.darkModeLabel, style = MaterialTheme.typography.titleMedium)
                                    Text("Switch between Dark Navy and Light Mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = darkThemeEnabled,
                                onCheckedChange = onToggleDarkTheme,
                                modifier = Modifier.testTag("dark_theme_switch")
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = NeonGreen)
                            Text(labels.languageLabel, style = MaterialTheme.typography.titleMedium)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UserPreferencesManager.supportedLanguages.forEach { (code, name) ->
                                FilterChip(
                                    selected = selectedLanguageCode == code,
                                    onClick = { onSelectLanguage(code) },
                                    label = { Text(name) },
                                    modifier = Modifier.testTag("lang_option_$code")
                                )
                            }
                        }
                    }
                }
            }

            // Security & Login Activity
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen)
                            Text("Account Security & Session", style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Signed-in Account: ${userEmail ?: "Google Account"}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "Last Active Login: ${profile?.lastLoginAt?.ifBlank { "Current Session" } ?: "Current Session"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonBlue
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Remember Session on Launch", style = MaterialTheme.typography.bodyMedium)
                                Text("Uses Credential Manager silent sign-in", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = rememberMeEnabled, onCheckedChange = onToggleRememberMe)
                        }
                        Text(
                            text = "Note: Passwordless Google Sign-In is protected by hardware/keystore attestation and Firebase App Check. Optional Email/Password or Phone OTP providers can be enabled in Firebase Console.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Privacy Policy, Terms & Support
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Legal, Compliance & Support", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = {
                                legalDialogTitle = "Privacy Policy"
                                legalDialogBody = "TejasCode collects only your Google account display name, email, learning progress, saved code snippets, and verified certificates inside your isolated Firestore workspace.\n\n" +
                                    "Payment Security: TejasCode NEVER collects, inspects, or stores card numbers, CVV, bank passwords, or UPI PINs. All payments are processed exclusively through Google Play Billing or PCI-DSS compliant Razorpay Checkout.\n\n" +
                                    "You may delete your account and associated workspace data at any time from Settings."
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("privacy_policy_button")
                        ) {
                            Icon(Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Privacy Policy")
                        }
                        OutlinedButton(
                            onClick = {
                                legalDialogTitle = "Terms, Cancellation & Refund Policy"
                                legalDialogBody = "1. Subscriptions renew automatically monthly or yearly unless cancelled at least 24 hours before the end of the current period.\n" +
                                    "2. Cancelled subscriptions retain full TejasCode Pro access until the paid billing cycle finishes.\n" +
                                    "3. Google Play purchases can be managed or refunded per Google Play policy; Razorpay refunds/revocations are synchronized via verified merchant webhooks.\n" +
                                    "4. Community Q&A posts must remain respectful and free of malicious code."
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("terms_button")
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Terms & Refund Policy")
                        }
                        OutlinedButton(
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:tejashpatel02010@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "TejasCode Support Request")
                                }
                                runCatching { context.startActivity(emailIntent) }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("contact_support_button")
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Contact Support (tejashpatel02010@gmail.com)")
                        }
                    }
                }
            }

            // Account Deletion
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, CoralRose.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Danger Zone", style = MaterialTheme.typography.titleMedium, color = CoralRose, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Permanently delete your TejasCode account, cloud snippets, and profile data.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRose),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_delete_account_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete My Account")
                        }
                    }
                }
            }
        }
    }
}
