package ai.sach.rentup.screens

import ai.sach.rentup.ui.theme.RentUpTheme
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Common Material components under RentUpTheme, light and dark, for reviewing theme changes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h860dp-xxhdpi")
class ThemeSpecimenTest {

    @get:Rule
    val compose = createComposeRule()
    private var dark by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun specimen() {
        val out = File(System.getProperty("shots.dir") ?: "build/outputs/shots").apply { mkdirs() }
        compose.setContent {
            RentUpTheme(darkTheme = dark) {
                val c = MaterialTheme.colorScheme
                Surface(Modifier.fillMaxSize(), color = c.background) {
                    Column {
                        TopAppBar(title = { Text("Properties") })
                        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Rent collected this month", style = MaterialTheme.typography.labelLarge)
                                    Text("₹48,500", style = MaterialTheme.typography.headlineMedium)
                                    Text("3 tenants overdue", color = c.error)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {}) { Text("Collect") }
                                FilledTonalButton(onClick = {}) { Text("Remind") }
                                OutlinedButton(onClick = {}) { Text("Edit") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(selected = true, onClick = {}, label = { Text("All") })
                                FilterChip(selected = false, onClick = {}, label = { Text("Overdue") })
                                TextButton(onClick = {}) { Text("Text button") }
                            }
                            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Tenant name") }, modifier = Modifier.fillMaxWidth())
                            ListItem(
                                leadingContent = { Box(Modifier.size(40.dp).background(c.primaryContainer, CircleShape)) },
                                headlineContent = { Text("Rahul Sharma") },
                                supportingContent = { Text("Flat 2B · due 5 Nov") },
                                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(c.primary, c.primaryContainer, c.secondary, c.secondaryContainer, c.tertiaryContainer, c.surfaceVariant, c.outline)
                                    .forEach { Box(Modifier.size(40.dp).background(it, CircleShape)) }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                FloatingActionButton(onClick = {}) { Icon(Icons.Default.Add, null) }
                            }
                        }
                        NavigationBar {
                            NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                            NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Tenants") })
                            NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                        }
                    }
                }
            }
        }
        for (mode in listOf("light", "dark")) {
            dark = mode == "dark"
            repeat(3) { Thread.sleep(200); compose.waitForIdle() }
            compose.onRoot().captureRoboImage(File(out, "theme_$mode.png").path)
        }
    }
}
