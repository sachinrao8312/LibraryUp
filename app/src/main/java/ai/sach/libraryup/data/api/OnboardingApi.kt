// Retrofit API interface for onboarding form link generation
// Used to generate unique tenant onboarding links via the backend
//
// See INTEGRATION_GUIDE.md for details
package ai.sach.rentup.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Data classes for request and response

data class GenerateLinkRequest(
    val ownerId: String,
    val propertyAddress: String,
    val deviceToken: String,
    val tenantId: String? = null  // Optional parameter for updating existing tenants
)

data class GenerateLinkResponse(
    val success: Boolean,
    val formId: String,
    val url: String,
    val expiresAt: String
)

enum class FileCategory { AADHAAR, PAN, RENT_AGREEMENT, OTHER_DOCUMENT, PROFILE_PHOTO, PROPERTY_PHOTO }

data class FileUpload(
    val file: ByteArray,
    val fileName: String,
    val category: FileCategory,
    val mimeType: String
)

interface OnboardingApi {
    /** [authorization] is "Bearer <Firebase ID token>"; the backend only mints links for that user. */
    @POST("/api/generate-link")
    suspend fun generateLink(
        @Header("Authorization") authorization: String,
        @Body request: GenerateLinkRequest
    ): Response<GenerateLinkResponse>
}

object OnboardingApiProvider {
    fun create(baseUrl: String): OnboardingApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OnboardingApi::class.java)
    }
}
