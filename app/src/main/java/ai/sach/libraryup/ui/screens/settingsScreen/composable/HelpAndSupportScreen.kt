package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.ui.library.LibraryTopBar
import ai.sach.libraryup.ui.theme.LibraryUpTheme
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

/**
 * Help and Support screen providing access to FAQs, support channels, and resources.
 *
 * @param navController Navigation controller for handling screen navigation
 */
@Composable
fun HelpAndSupportScreen(navController: NavController) {
    val context = LocalContext.current

    // FAQ state
    val faqs = remember {
        listOf(
            FAQ("How do I add a student?",
                "Tap Students > Add student (or the quick action on Home). Pick the shift, a free seat " +
                        "and the plan, then save. You can record the first payment on the same screen."),
            FAQ("How do I collect a fee and send a receipt?",
                "Open the student and tap 'Collect fee'. After saving, the receipt PDF opens in the " +
                        "share sheet - choose WhatsApp to send it."),
            FAQ("How do I see which seats are free in a shift?",
                "Open Seats and tap the shift at the top. Green seats are free in that shift."),
            FAQ("How do I send fee reminders to everyone?",
                "More > Fee reminders. Tap 'Send' - WhatsApp opens with the message ready. Send it, " +
                        "come back and tap 'Send next'. No message credits, it uses your own WhatsApp."),
            FAQ("Can I export my data to Excel?",
                "Yes: More > Import & export. Students and payments export as CSV files that open in Excel."),
            FAQ("Where is my data stored?",
                "On this phone, and in your own Google Drive if you turn on Drive backup. LibraryUp has " +
                        "no server holding your students' data. Turn on password protection in Backup & " +
                        "Restore to encrypt backups.")
        )
    }

    // Track expanded FAQs
    var expandedFaqIndex by remember { mutableIntStateOf(-1) }

    // Contact dialog state
    var showContactDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LibraryTopBar("Help & Support", onBack = { navController.popBackStack() })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Support options
            SupportOptionsSection()

            // FAQs section
            FaqsSection(
                faqs = faqs,
                expandedIndex = expandedFaqIndex,
                onExpandToggle = { index ->
                    expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
                }
            )

            // Contact options
            ContactSection(
                onContactEmailClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:thetechgovernor@gmail.com".toUri()
                        putExtra(Intent.EXTRA_SUBJECT, "LibraryUp Support Request")
                    }
                    context.startActivity(intent)
                },
                onContactChatClick = {
                    showContactDialog = true
                },
                onTelegramClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://t.me/skiiingg".toUri())
                    context.startActivity(intent)
                }
            )

            // Resources section
            ResourcesSection(
                onDocumentationClick = { /* Open documentation */ },
                onTutorialClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://youtube.com/playlist?list=PLOj8YOsTl0qmAnFSJBnlk39jwuepylsYl".toUri())
                    context.startActivity(intent)
                },
                onPrivacyPolicyClick = { navController.navigate("privacy") }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Live chat dialog
        if (showContactDialog) {
            AlertDialog(
                onDismissRequest = { showContactDialog = false },
                title = { Text("Contact Support") },
                text = {
                    Column {
                        Text("Choose a support channel:")

                        Spacer(modifier = Modifier.height(16.dp))



                        ContactOption(
                            icon = Icons.Default.Email,
                            title = "Email Support",
                            subtitle = "thetechgovernor@gmail.com",
                            onClick = {
                                showContactDialog = false
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = "mailto:thetechgovernor@gmail.com".toUri()
                                    putExtra(Intent.EXTRA_SUBJECT, "LibraryUp Support Request")
                                }
                                context.startActivity(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ContactOption(
                            icon = Icons.Default.Phone,
                            title = "Phone Support",
                            subtitle = "+1 (555) 123-4567",
                            onClick = {
                                showContactDialog = false
                                val intent = Intent(Intent.ACTION_DIAL, "tel:+15551234567".toUri())
                                context.startActivity(intent)
                            }
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showContactDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SupportOptionsSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "How can we help you?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Find answers to common questions in our FAQ section below or contact our support team for personalized assistance.",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun FaqsSection(
    faqs: List<FAQ>,
    expandedIndex: Int,
    onExpandToggle: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Frequently Asked Questions",
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                faqs.forEachIndexed { index, faq ->
                    val isExpanded = index == expandedIndex

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExpandToggle(index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Q",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = faq.question,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp)
                            )

                            Icon(
                                imageVector = if (isExpanded)
                                    Icons.Default.KeyboardArrowUp
                                else
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = Color.Gray
                            )
                        }

                        // Answer (expanded content)
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 64.dp, end = 16.dp, bottom = 16.dp)
                            ) {
                                Text(
                                    text = faq.answer,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Add divider between items (but not after the last one)
                    if (index < faqs.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactSection(
    onContactEmailClick: () -> Unit,
    onContactChatClick: () -> Unit,
    onTelegramClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Contact Support",
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                SupportOptionItem(
                    icon = Icons.Default.Email,
                    title = "Email Support",
                    subtitle = "Get help via email",
                    onClick = onContactEmailClick
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SupportOptionItem(
                    icon = Icons.AutoMirrored.Filled.Message,
                    title = "Telegram",
                    subtitle = "@kitretsuuuuu",
                    onClick = onTelegramClick
                )
            }
        }
    }
}

@Composable
private fun ResourcesSection(
    onDocumentationClick: () -> Unit,
    onTutorialClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Resources",
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                SupportOptionItem(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Documentation",
                    subtitle = "Read the user manual",
                    onClick = onDocumentationClick
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SupportOptionItem(
                    icon = Icons.Default.OndemandVideo,
                    title = "Video Tutorials",
                    subtitle = "Learn with step-by-step guides",
                    onClick = onTutorialClick
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SupportOptionItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "View our privacy policy",
                    onClick = onPrivacyPolicyClick
                )
            }
        }
    }
}

@Composable
private fun SupportOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@Composable
private fun ContactOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Data class representing an FAQ item.
 */
data class FAQ(
    val question: String,
    val answer: String
)


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HelpAndSupportScreenPreview() {
    // It's a good practice to wrap previews in your app's theme
    // to ensure styling (colors, fonts, etc.) is applied correctly.
    LibraryUpTheme {
        HelpAndSupportScreen(navController = rememberNavController())
    }
}