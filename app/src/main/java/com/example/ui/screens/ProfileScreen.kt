package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Certificate
import com.example.model.CommunityProject
import com.example.model.UiState
import com.example.model.UserEntitlement
import com.example.model.UserProfile
import com.example.ui.components.SyntaxCodeCard
import com.example.ui.theme.CoralRose
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.util.CertificateExportHelper
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profileState: UiState<UserProfile?>,
    entitlementState: UiState<UserEntitlement>,
    certificatesState: UiState<List<Certificate>>,
    communityState: UiState<List<CommunityProject>>,
    currentUserId: String,
    userEmail: String?,
    isAdmin: Boolean,
    onCompleteRegistration: (displayName: String, learningGoal: String, experienceLevel: String) -> Unit,
    onUpdateExtendedProfile: (bio: String, preferredLanguage: String) -> Unit,
    onOpenPremium: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    onToggleStar: (CommunityProject) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = (profileState as? UiState.Success)?.data
    val entitlement = (entitlementState as? UiState.Success)?.data ?: UserEntitlement()

    var showEditRegistration by remember { mutableStateOf(false) }
    var showEditBio by remember { mutableStateOf(false) }
    var bioInput by remember(profile?.bio) { mutableStateOf(profile?.bio.orEmpty()) }

    if (showEditRegistration) {
        StudentRegistrationDialog(
            initialName = profile?.displayName ?: "",
            initialGoal = profile?.learningGoal ?: "HTML, CSS & JavaScript",
            initialLevel = profile?.experienceLevel ?: "Beginner",
            dismissible = true,
            onDismiss = { showEditRegistration = false },
            onSubmit = { name, goal, level ->
                onCompleteRegistration(name, goal, level)
                showEditRegistration = false
            }
        )
    }

    if (showEditBio) {
        AlertDialog(
            onDismissRequest = { showEditBio = false },
            title = { Text("Edit Developer Bio") },
            text = {
                OutlinedTextField(
                    value = bioInput,
                    onValueChange = { if (it.length <= 280) bioInput = it },
                    label = { Text("Bio (skills, goals, links)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("bio_input_field")
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateExtendedProfile(bioInput.trim(), profile?.preferredLanguage ?: "en")
                    showEditBio = false
                }) { Text("Save Bio") }
            },
            dismissButton = {
                TextButton(onClick = { showEditBio = false }) { Text("Cancel") }
            }
        )
    }

    val codingHours = ((profile?.totalCodingMinutes ?: 0L) / 60.0)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_tejas_coder),
                                contentDescription = "TejasCode Student Avatar",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = profile?.displayName ?: "TejasCode Student",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = (if (entitlement.isProActive) NeonGreen else NeonBlue).copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = if (entitlement.isProActive) "PRO" else "FREE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (entitlement.isProActive) NeonGreen else NeonBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (!userEmail.isNullOrBlank()) {
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.height(48.dp).testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }

                    // Bio section
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = profile?.bio?.ifBlank { "Add a developer bio to showcase on TejasCode..." }
                                    ?: "Add a developer bio to showcase on TejasCode...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showEditBio = true }, modifier = Modifier.testTag("edit_bio_button")) {
                                Text("Bio")
                            }
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Track: ${profile?.learningGoal ?: "HTML, CSS & JavaScript"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonBlue
                                )
                                Text(
                                    text = "Level: ${profile?.experienceLevel ?: "Beginner"} • Saved Courses: ${profile?.savedCourseIds?.size ?: 0}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { showEditRegistration = true },
                                modifier = Modifier.testTag("edit_registration_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit student registration", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile("XP", "${profile?.xp ?: 0}", NeonBlue, Modifier.weight(1f))
                StatTile("Streak", "${profile?.streakDays ?: 1}d", NeonAmber, Modifier.weight(1f))
                StatTile("Hours", String.format(Locale.US, "%.1fh", codingHours), NeonGreen, Modifier.weight(1f))
                StatTile("Modules", "${profile?.lessonsCompleted ?: 0}", NeonPurple, Modifier.weight(1f))
            }
        }

        // Achievement Badges
        val badges = profile?.badges.orEmpty()
        if (badges.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Achievements & Badges", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        badges.forEach { badge ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = NeonAmber.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonAmber,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pro Billing & Settings Shortcuts
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpenPremium,
                    modifier = Modifier.weight(1f).height(50.dp).testTag("profile_premium_button")
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (entitlement.isProActive) "Manage Pro" else "Go Pro", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f).height(50.dp).testTag("profile_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Settings & Legal", maxLines = 1)
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Course Manager",
                            tint = NeonPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text("Admin Control Console", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (isAdmin) "Verified Admin • Courses, Pricing & Settlements" else "View instructor management status",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenAdmin,
                        modifier = Modifier.height(48.dp).testTag("profile_admin_button")
                    ) {
                        Text("Open")
                    }
                }
            }
        }

        item {
            Text(
                text = "Verified Certificates",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        when (certificatesState) {
            is UiState.Loading -> item { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
            is UiState.Error -> item { Text(certificatesState.message, color = CoralRose) }
            is UiState.Success -> {
                if (certificatesState.data.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = NeonAmber,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Complete all modules in any TejasCode track to unlock a verified QR + PDF certificate.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(certificatesState.data, key = { it.id }) { cert ->
                        CertificateCard(
                            cert = cert,
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "I earned the Verified ${cert.trackTitle} Certificate on TejasCode! Verification ID: ${cert.verificationCode}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Certificate"))
                            },
                            onExportPdf = {
                                CertificateExportHelper.exportCertificatePdfAndShare(context, cert)
                                    .onFailure {
                                        Toast.makeText(context, "PDF export error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Community Code Showcase",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        when (communityState) {
            is UiState.Loading -> item { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
            is UiState.Error -> item { Text(communityState.message, color = CoralRose) }
            is UiState.Success -> {
                if (communityState.data.isEmpty()) {
                    item {
                        Text(
                            text = "No shared projects yet. Publish your first snippet from the Code Editor!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(communityState.data, key = { it.id }) { project ->
                        val isStarred = currentUserId in project.likedBy
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(project.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "by ${project.authorName} • ${project.language}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeonBlue
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onToggleStar(project) }) {
                                            Icon(
                                                imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Star project",
                                                tint = if (isStarred) NeonAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text("${project.likesCount}", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                if (project.description.isNotBlank()) {
                                    Text(project.description, style = MaterialTheme.typography.bodySmall)
                                }
                                SyntaxCodeCard(code = project.code, languageLabel = project.language)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun CertificateCard(
    cert: Certificate,
    onShare: () -> Unit,
    onExportPdf: () -> Unit
) {
    val qrBitmap = remember(cert.verificationCode) {
        CertificateExportHelper.generateQrBitmap("https://tejascode.dev/verify/${cert.verificationCode}", 180)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_certificate_crest),
                    contentDescription = "Certificate Crest",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cert.trackTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonAmber
                    )
                    Text(
                        text = "Awarded to ${cert.recipientName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "ID: ${cert.verificationCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonBlue
                    )
                }
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Certificate Verification QR Code",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("share_cert_${cert.id}")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("pdf_cert_${cert.id}")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save PDF", maxLines = 1)
                }
            }
        }
    }
}
