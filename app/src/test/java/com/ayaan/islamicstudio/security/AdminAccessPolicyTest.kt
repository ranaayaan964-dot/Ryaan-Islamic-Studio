package com.ayaan.islamicstudio.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AdminAccessPolicyTest {
    @Test
    fun verifiedAllowlistedEmailsAreRequiredWithoutAnAdminClaim() {
        AdminAccessPolicy.authorizedEmails.forEach { email ->
            assertTrue(AdminAccessPolicy.isAuthorized(" ${email.uppercase(Locale.US)} ", true, false))
            assertFalse(AdminAccessPolicy.isAuthorized(email, false, false))
        }
        assertFalse(AdminAccessPolicy.isAuthorized("other@example.com", true, false))
        assertFalse(AdminAccessPolicy.isAuthorized(null, true, false))
    }

    @Test
    fun adminClaimDoesNotRequireAVerifiedEmail() {
        assertTrue(AdminAccessPolicy.isAuthorized(null, false, true))
    }
}
