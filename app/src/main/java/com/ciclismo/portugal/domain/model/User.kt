package com.ciclismo.portugal.domain.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val isPremium: Boolean = false,
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        // List of admin emails - add your email here
        private val ADMIN_EMAILS = listOf(
            "app.cyclingai@gmail.com",
            "brunomigmarques@gmail.com"
        )

        fun isAdminEmail(email: String): Boolean {
            return ADMIN_EMAILS.any { it.equals(email, ignoreCase = true) }
        }
    }
}
