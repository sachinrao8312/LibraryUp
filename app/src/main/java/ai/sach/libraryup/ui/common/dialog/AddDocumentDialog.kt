package ai.sach.rentup.ui.common.dialog

import ai.sach.rentup.data.local.entity.TenantDocument
import ai.sach.rentup.ui.common.document.DocumentPickerButton
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun AddDocumentDialog(
    tenantId: Long,
    onDismiss: () -> Unit,
    onAddDocument: (TenantDocument) -> Unit
) {
    var documentType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var documentPath by remember { mutableStateOf("") }
    var documentName by remember { mutableStateOf("") }
    var documentMimeType by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Document") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = documentType,
                    onValueChange = { documentType = it },
                    label = { Text("Document Type") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Use DocumentPickerButton with selected filename display
                DocumentPickerButton(
                    onDocumentSelected = { uri, name, mimeType ->
                        documentPath = uri.toString()
                        documentName = name
                        documentMimeType = mimeType ?: if (name.endsWith(".pdf", true)) "application/pdf" else ""
                    },
                    selectedFileName = documentName
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (documentType.isNotBlank() && documentPath.isNotBlank()) {
                        val document = TenantDocument(
                            tenantId = tenantId,
                            documentType = documentType,
                            uploadDate = LocalDate.now().toString(),
                            documentPath = documentPath,
                            description = description.ifBlank { null },
                            mimeType = documentMimeType.ifBlank { null }
                        )
                        onAddDocument(document)
                    } else {
                        Toast.makeText(
                            context,
                            "Please complete all required fields",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = documentType.isNotBlank() && documentPath.isNotBlank()
            ) {
                Text("Add Document")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}