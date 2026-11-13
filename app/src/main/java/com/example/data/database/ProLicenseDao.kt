package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProLicenseDao {
    @Query("SELECT * FROM pro_license_keys ORDER BY createdAt DESC")
    fun getAllProKeysFlow(): Flow<List<ProLicenseKeyEntity>>

    @Query("SELECT * FROM pro_license_keys WHERE UPPER(licenseKey) = UPPER(:key) LIMIT 1")
    suspend fun getProKeyByCode(key: String): ProLicenseKeyEntity?

    @Query("SELECT * FROM pro_license_keys WHERE isUsed = :used ORDER BY createdAt DESC")
    suspend fun getProKeysByUsage(used: Boolean): List<ProLicenseKeyEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProKey(proKey: ProLicenseKeyEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProKeys(proKeys: List<ProLicenseKeyEntity>)

    @Query("UPDATE pro_license_keys SET isUsed = 1, usedByUsername = :username, usedByEmail = :email, usedByUid = :uid, activatedAt = :activatedAt, expiresAt = :expiresAt WHERE UPPER(licenseKey) = UPPER(:key) AND isUsed = 0")
    suspend fun markKeyUsedAtomically(
        key: String,
        username: String,
        email: String,
        uid: String,
        activatedAt: Long,
        expiresAt: Long
    ): Int

    @Query("DELETE FROM pro_license_keys WHERE licenseKey = :key")
    suspend fun deleteProKey(key: String)
}
