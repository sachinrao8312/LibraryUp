package ai.sach.libraryup.ui.screens.authentication

/**
 * Data class to hold user information including profile photo URL.
 */
data class UserData(
    val name: String,
    val email: String? = null,
    val phoneNumber: String = "",
    val photoUrl: String? = null
) 