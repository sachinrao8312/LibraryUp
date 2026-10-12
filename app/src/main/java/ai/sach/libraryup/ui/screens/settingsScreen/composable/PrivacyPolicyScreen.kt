package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.ui.common.composables.AppTopAppBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Privacy Policy screen displaying the app's privacy information,
 * including device fingerprinting disclosures to prevent authentication abuse.
 *
 * @param navController Navigation controller for handling screen navigation
 */
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    val lastUpdated = "September 28, 2026"

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = "Privacy Policy",
                showBackButton = true,
                onBackPressed = { navController.popBackStack() },
                showDefaultMenu = true,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Privacy header card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    ) {
                        Text(
                            text = "Your Privacy Matters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Last updated: $lastUpdated",
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Main sections
            PrivacySection(
                title = "Introduction",
                content = """
                    At RentUp, we respect your privacy and are committed to protecting your personal data. 
                    This policy outlines how we collect, use, and secure your information.
                """.trimIndent()
            )

            PrivacySection(
                title = "Data We Collect",
                content = """
                    • Personal details (Name, Email, Phone Number)
                    • Tenant and property details
                    • Application usage statistics
                    • Device information including device identifiers for security purposes
                    
                    Your tenant, property and payment records are stored on your device and are
                    not uploaded to RentUp's servers. Earlier versions copied them to our cloud
                    database for users signed in with Google - contact us to have those copies
                    deleted.
                """.trimIndent()
            )

            PrivacySection(
                title = "Device Fingerprinting",
                content = """
                    To protect against authentication abuse, we use device fingerprinting technologies. 
                    This involves collecting technical data from your device, such as:
                    
                    • Device type and model
                    • Operating system version
                    • App version
                    • Device identifiers (non-personal)
                    
                    This information is used solely for fraud prevention, account security, and ensuring fair usage.
                    It is never sold or shared for advertising purposes.
                """.trimIndent()
            )

            PrivacySection(
                title = "How We Use Your Data",
                content = """
                    • To deliver core app functionalities
                    • To improve user experience and app reliability
                    • To provide customer support
                    • To secure accounts through anti-abuse mechanisms
                    
                    We never sell your data to third parties.
                """.trimIndent()
            )

            PrivacySection(
                title = "Data Storage and Security",
                content = """
                    • Tenant, property and payment records are stored in the app's private storage on your device.
                    • Android's system backup may copy this data to your Google account, following your device's backup settings.
                    • If you turn on Google Drive backup, a backup file is saved to a "RentUp Backups" folder in your own Google Drive. RentUp can only see files it created there.
                    • Backup files you save yourself are not encrypted - keep them somewhere private.
                    • Details and files a tenant submits through an onboarding link are stored on our servers (Firebase and Cloudflare R2) so the app can import them.
                """.trimIndent()
            )

            PrivacySection(
                title = "Your Rights",
                content = """
                    You have the right to:
                    • Access your data
                    • Correct inaccuracies
                    • Delete your data
                    • Restrict processing
                    • Export your data
                    
                    Manage your data directly through the app settings.
                """.trimIndent()
            )

            PrivacySection(
                title = "Changes to This Policy",
                content = """
                    We reserve the right to update this policy as needed.
                    Any changes will be posted on our official Privacy Policy page hosted at:                                      
                    Please review it periodically.
                """.trimIndent()
            )

            PrivacySection(
                title = "Contact Us",
                content = """
                    Have questions? Reach out to us:
                    
                    📧 Email: thetechgovernor@gmail.com
                    📞 Or contact us directly via the app's Support section.
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(16.dp),
                lineHeight = 24.sp
            )
        }
    }
}
