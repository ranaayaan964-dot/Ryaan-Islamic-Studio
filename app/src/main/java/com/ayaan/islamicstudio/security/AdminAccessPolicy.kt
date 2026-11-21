package com.ayaan.islamicstudio.security

import java.util.Locale

internal object AdminAccessPolicy {
    val authorizedEmails = setOf(
        "ayianrana20133@gmail.com",
        "ranasalar964@gmail.com"
    )

    fun isAuthorized(email: String?, emailVerified: Boolean, hasAdminClaim: Boolean): Boolean =
        hasAdminClaim ||
            (emailVerified && email?.trim()?.lowercase(Locale.US) in authorizedEmails)
}
