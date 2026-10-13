package ai.sach.libraryup.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object ImageUtils {
    private const val TAG = "ImageUtils"
    
    // Configurable constants
    object Config {
        const val DEFAULT_MAX_IMAGE_SIZE = 1024
        const val DEFAULT_COMPRESSION_QUALITY = 85
        const val MAX_CACHE_SIZE = 10 * 1024 * 1024 // 10MB
        const val MIN_SAMPLE_SIZE = 1
        const val MAX_SAMPLE_SIZE = 8
    }

    /**
     * Loads a bitmap from a URI with proper memory management and error recovery
     */
    fun loadBitmapFromUri(
        context: Context, 
        uri: Uri, 
        maxSize: Int = Config.DEFAULT_MAX_IMAGE_SIZE,
        config: Bitmap.Config = Bitmap.Config.RGB_565
    ): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            val contentResolver: ContentResolver = context.contentResolver
            
            // Get the input stream
            inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $uri")
                return null
            }

            // Decode with options to reduce memory usage
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            inputStream = null

            // Calculate sample size
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxSize)
            
            // Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = config
                // Note: inPurgeable and inInputShareable are deprecated but still functional
                // They will be replaced with newer memory management APIs in future Android versions
                inPurgeable = true // Allow the system to purge the bitmap if needed
                inInputShareable = true // Share the input data
            }

            inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            
            if (bitmap == null) {
                Log.w(TAG, "Failed to decode bitmap, trying with ARGB_8888 config")
                // Fallback to ARGB_8888 if RGB_565 fails
                return loadBitmapFromUri(context, uri, maxSize, Bitmap.Config.ARGB_8888)
            }
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI: $uri", e)
            null
        } finally {
            inputStream?.close()
        }
    }

    /**
     * Saves a bitmap to a file with compression
     */
    fun saveBitmapToFile(
        bitmap: Bitmap, 
        file: File, 
        quality: Int = Config.DEFAULT_COMPRESSION_QUALITY
    ): Boolean {
        var outputStream: FileOutputStream? = null
        return try {
            outputStream = FileOutputStream(file)
            val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            if (!compressed) {
                Log.w(TAG, "Failed to compress bitmap")
            }
            compressed
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to file", e)
            false
        } finally {
            outputStream?.close()
        }
    }

    /**
     * Creates a content URI for a file using FileProvider
     */
    fun getContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Calculates the appropriate sample size for bitmap decoding
     */
    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = Config.MIN_SAMPLE_SIZE
        while (sampleSize < Config.MAX_SAMPLE_SIZE && 
               (width / sampleSize > maxSize || height / sampleSize > maxSize)) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * Recycles a bitmap safely
     */
    fun recycleBitmap(bitmap: Bitmap?) {
        try {
            bitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error recycling bitmap", e)
        }
    }

    /**
     * Converts bitmap to byte array with compression
     */
    fun bitmapToByteArray(
        bitmap: Bitmap, 
        quality: Int = Config.DEFAULT_COMPRESSION_QUALITY
    ): ByteArray? {
        return try {
            val outputStream = ByteArrayOutputStream()
            val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            if (compressed) {
                outputStream.toByteArray()
            } else {
                Log.w(TAG, "Failed to compress bitmap to byte array")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to byte array", e)
            null
        }
    }

    /**
     * Loads bitmap with automatic format detection and optimization
     */
    fun loadOptimizedBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            // Try with RGB_565 first (less memory)
            loadBitmapFromUri(context, uri, Config.DEFAULT_MAX_IMAGE_SIZE, Bitmap.Config.RGB_565)
                ?: // Fallback to ARGB_8888 if RGB_565 fails
                loadBitmapFromUri(context, uri, Config.DEFAULT_MAX_IMAGE_SIZE, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading optimized bitmap", e)
            null
        }
    }
}