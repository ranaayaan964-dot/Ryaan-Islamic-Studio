package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProPlanTier(
    val tierId: String,
    val displayName: String,
    val keyPrefix: String,
    val durationDays: Int,
    val pricePkr: Int,
    val priceFormatted: String,
    val badgeLabel: String
) {
    WEEKLY("PRO_WEEKLY", "Weekly Pro Plan", "WEEK", 7, 1000, "Rs. 1,000 PKR", "7 Days Access"),
    MONTHLY("PRO_MONTHLY", "Monthly Pro Plan", "MNTH", 30, 3000, "Rs. 3,000 PKR", "30 Days Access • Popular"),
    YEARLY("PRO_YEARLY", "Yearly Pro Plan", "YEAR", 365, 25000, "Rs. 25,000 PKR", "365 Days • Best Value (Save Rs. 11,000)")
}

@Entity(tableName = "pro_license_keys")
data class ProLicenseKeyEntity(
    @PrimaryKey val licenseKey: String,
    val planTier: String,
    val durationDays: Int,
    val pricePkr: Int,
    val isUsed: Boolean = false,
    val usedByUsername: String = "",
    val usedByEmail: String = "",
    val usedByUid: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val activatedAt: Long = 0L,
    val expiresAt: Long = 0L,
    val aiNote: String = ""
)
