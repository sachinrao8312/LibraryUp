package ai.sach.libraryup.utils

import ai.sach.libraryup.data.api.FileCategory
import ai.sach.libraryup.data.api.FileUpload
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.io.InputStream

object FileUploadUtils {
    
    /**
     * Convert URI to FileUpload object
     */
    fun uriToFileUpload(
        context: Context,
        uri: Uri,
        category: FileCategory
    ): FileUpload? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return null
            
            // Get file name
            val fileName = getFileName(context, uri) ?: "unknown_file"
            
            // Get MIME type
            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromExtension(fileName)
            
            // Convert to byte array
            val byteArray = inputStream.use { stream ->
                val buffer = ByteArrayOutputStream()
                val data = ByteArray(1024)
                var nRead: Int
                while (stream.read(data, 0, data.size).also { nRead = it } != -1) {
                    buffer.write(data, 0, nRead)
                }
                buffer.toByteArray()
            }
            
            FileUpload(
                file = byteArray,
                fileName = fileName,
                category = category,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get file name from URI
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
        }
        
        if (fileName == null) {
            fileName = uri.path
            val cut = fileName?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                fileName = fileName.substring(cut + 1)
            }
        }
        
        return fileName
    }
    
    /**
     * Get MIME type from file extension
     */
    private fun getMimeTypeFromExtension(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) 
            ?: "application/octet-stream"
    }
    
    /**
     * Validate file size (in bytes)
     */
    fun isFileSizeValid(fileSize: Long, maxSizeBytes: Long = 10 * 1024 * 1024): Boolean {
        return fileSize <= maxSizeBytes
    }
    
    /**
     * Validate file type for category with magic number verification
     */
    fun isFileTypeValid(mimeType: String, category: FileCategory, fileData: ByteArray? = null): Boolean {
        val mimeTypeValid = when (category) {
            FileCategory.AADHAAR, FileCategory.PAN, FileCategory.RENT_AGREEMENT, FileCategory.OTHER_DOCUMENT -> {
                mimeType.startsWith("image/") || mimeType == "application/pdf"
            }
            FileCategory.PROFILE_PHOTO, FileCategory.PROPERTY_PHOTO -> {
                mimeType.startsWith("image/")
            }
        }
        
        // Additional magic number validation if file data is provided
        if (fileData != null && fileData.isNotEmpty()) {
            return mimeTypeValid && isValidFileHeader(fileData, mimeType)
        }
        
        return mimeTypeValid
    }
    
    /**
     * Validate file header (magic numbers) to prevent MIME type spoofing
     */
    private fun isValidFileHeader(fileData: ByteArray, mimeType: String): Boolean {
        if (fileData.size < 4) return false
        
        return when (mimeType) {
            "image/jpeg" -> {
                // JPEG magic numbers: FF D8 FF
                fileData[0] == 0xFF.toByte() && fileData[1] == 0xD8.toByte() && fileData[2] == 0xFF.toByte()
            }
            "image/png" -> {
                // PNG magic numbers: 89 50 4E 47
                fileData[0] == 0x89.toByte() && fileData[1] == 0x50.toByte() && 
                fileData[2] == 0x4E.toByte() && fileData[3] == 0x47.toByte()
            }
            "application/pdf" -> {
                // PDF magic numbers: 25 50 44 46 (%PDF)
                fileData[0] == 0x25.toByte() && fileData[1] == 0x50.toByte() && 
                fileData[2] == 0x44.toByte() && fileData[3] == 0x46.toByte()
            }
            "image/gif" -> {
                // GIF magic numbers: 47 49 46 38 (GIF8)
                fileData[0] == 0x47.toByte() && fileData[1] == 0x49.toByte() && 
                fileData[2] == 0x46.toByte() && fileData[3] == 0x38.toByte()
            }
            else -> true // Allow other types to pass basic validation
        }
    }
    
    /**
     * Get human readable file size
     */
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
    
    /**
     * Get category display name
     */
    fun getCategoryDisplayName(category: FileCategory): String {
        return when (category) {
            FileCategory.AADHAAR -> "Aadhaar Card"
            FileCategory.PAN -> "PAN Card"
            FileCategory.RENT_AGREEMENT -> "Rent Agreement"
            FileCategory.OTHER_DOCUMENT -> "Other Document"
            FileCategory.PROFILE_PHOTO -> "Profile Photo"
            FileCategory.PROPERTY_PHOTO -> "Property Photo"
        }
    }
    
    /**
     * Validate total upload size
     */
    fun isTotalSizeValid(files: List<FileUpload>, maxTotalSizeBytes: Long = 50 * 1024 * 1024): Boolean {
        val totalSize = files.sumOf { it.file.size.toLong() }
        return totalSize <= maxTotalSizeBytes
    }
}