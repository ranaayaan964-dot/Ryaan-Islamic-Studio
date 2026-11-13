package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.database.ProLicenseKeyEntity
import com.example.data.database.ProPlanTier
import com.example.data.database.UserEntity
import com.example.data.remote.DualAiScholarEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ProStatusState(
    val isProActive: Boolean = false,
    val activePlanName: String = "Free Plan",
    val proExpiresAt: Long = 0L,
    val remainingDays: Int = 0,
    val activatedKey: String = ""
)

class ProSubscriptionRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val userDao = database.userDao()
    private val keyDao = database.proLicenseDao()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val preferences: SharedPreferences =
        appContext.getSharedPreferences("ayaan_free_tier_limits_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableProStatus = MutableStateFlow(ProStatusState())
    val proStatusFlow: StateFlow<ProStatusState> = mutableProStatus.asStateFlow()

    init {
        scope.launch {
            userDao.getCurrentUserFlow().collect { user ->
                val admin = runCatching { hasAuthorizedAdminClaim() }.getOrDefault(false)
                mutableProStatus.value = user.toProStatus(admin)
            }
        }
        scope.launch {
            auth.addIdTokenListener(FirebaseAuth.IdTokenListener { scope.launch { val user = runCatching { userDao.getCurrentUser() }.getOrNull(); mutableProStatus.value = user.toProStatus(runCatching { hasAuthorizedAdminClaim() }.getOrDefault(false)) } })
        }
    }

    fun canUseFeatureOrConsume(featureKey: String, dailyLimit: Int): Boolean = synchronized(preferences) {
        if (proStatusFlow.value.isProActive) return@synchronized true
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val userKey = currentUsageUserKey()
        val dayPreferenceKey = "$KEY_USAGE_DAY_PREFIX$userKey"
        val storedDay = preferences.getString(dayPreferenceKey, null)
        if (storedDay != today) {
            val editor = preferences.edit()
            preferences.all.keys
                .filter { it.startsWith("$KEY_USAGE_PREFIX${userKey}_") }
                .forEach { editor.remove(it) }
            editor.putString(dayPreferenceKey, today).apply()
        }
        val key = usageCountKey(userKey, featureKey)
        val count = preferences.getInt(key, 0)
        if (count >= dailyLimit) return@synchronized false
        preferences.edit().putInt(key, count + 1).apply()
        true
    }

    fun remainingUses(featureKey: String, dailyLimit: Int): Int {
        if (proStatusFlow.value.isProActive) return Int.MAX_VALUE
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val userKey = currentUsageUserKey()
        if (preferences.getString("$KEY_USAGE_DAY_PREFIX$userKey", null) != today) return dailyLimit
        return (dailyLimit - preferences.getInt(usageCountKey(userKey, featureKey), 0)).coerceAtLeast(0)
    }

    private fun currentUsageUserKey(): String =
        auth.currentUser?.uid?.takeIf(String::isNotBlank) ?: "anonymous"

    private fun usageCountKey(userKey: String, featureKey: String): String =
        "$KEY_USAGE_PREFIX${userKey}_${featureKey.trim().lowercase(Locale.US)}"

    fun observeKeys() = keyDao.getAllProKeysFlow()

    suspend fun syncKeysForAdmin() {
        requireAdmin()
        val snapshot = firestore.collection(KEY_COLLECTION).get().await()
        val keys = snapshot.documents.mapNotNull { document ->
            val plan = document.getString("planTier") ?: return@mapNotNull null
            ProLicenseKeyEntity(
                licenseKey = document.getString("licenseKey") ?: document.id,
                planTier = plan,
                durationDays = (document.getLong("durationDays") ?: return@mapNotNull null).toInt(),
                pricePkr = (document.getLong("pricePkr") ?: 0L).toInt(),
                isUsed = document.getBoolean("isUsed") ?: false,
                usedByUsername = document.getString("usedByUsername").orEmpty(),
                usedByEmail = document.getString("usedByEmail").orEmpty(),
                usedByUid = document.getString("usedByUid").orEmpty(),
                createdAt = document.getLong("createdAt") ?: 0L,
                activatedAt = document.getLong("activatedAt") ?: 0L,
                expiresAt = document.getLong("expiresAt") ?: 0L,
                aiNote = document.getString("aiNote").orEmpty()
            )
        }
        keyDao.insertProKeys(keys)
    }

    suspend fun generateProKeyWithGroqAi(
        tier: ProPlanTier,
        buyerNote: String = ""
    ): Result<ProLicenseKeyEntity> =
        runCatching {
            requireAdmin()
            val key = createUniqueKey(tier)
            val fallbackNote = "Alhamdulillah! Your ${tier.displayName} gives you ${tier.durationDays} days of premium access."
            val note = buyerNote.trim().take(120).ifBlank {
                createGroqWelcomeNote(tier).onFailure {
                    Log.w(TAG, "Groq welcome note unavailable; using the standard activation message.", it)
                }.getOrDefault(fallbackNote)
            }
            val entity = ProLicenseKeyEntity(
                licenseKey = key,
                planTier = tier.tierId,
                durationDays = tier.durationDays,
                pricePkr = tier.pricePkr,
                createdAt = System.currentTimeMillis(),
                aiNote = note
            )
            firestore.collection(KEY_COLLECTION).document(key)
                .set(entity.toFirestore())
                .await()
            keyDao.insertProKey(entity)
            entity
        }

    suspend fun generateProKey(tier: ProPlanTier, buyerNote: String = ""): Result<ProLicenseKeyEntity> =
        generateProKeyWithGroqAi(tier, buyerNote)

    private suspend fun createGroqWelcomeNote(tier: ProPlanTier): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val key = DualAiScholarEngine.groqApiKeyPool().firstOrNull()
                ?: error("No Groq API key is configured for welcome-note generation.")
            val prompt = JSONObject()
                .put("model", "llama-3.3-70b-versatile")
                .put("temperature", 0.7)
                .put("max_tokens", 100)
                .put(
                    "messages",
                    org.json.JSONArray()
                        .put(
                            JSONObject()
                                .put("role", "system")
                                .put("content", "Write one warm, concise Islamic welcome note for a Pro subscription. Do not generate or include activation keys.")
                        )
                        .put(
                            JSONObject()
                                .put("role", "user")
                                .put("content", "Plan: ${tier.displayName} (${tier.priceFormatted}), access duration: ${tier.durationDays} days.")
                        )
                )
                .toString()
            val request = Request.Builder()
                .url(GROQ_CHAT_ENDPOINT)
                .header("Authorization", "Bearer $key")
                .post(prompt.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            groqHttpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Groq note request failed with HTTP ${response.code}." }
                val responseBody = response.body?.string().orEmpty()
                val note = JSONObject(responseBody)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
                    .take(120)
                check(note.isNotBlank()) { "Groq returned an empty welcome note." }
                note
            }
        }
    }

    suspend fun redeemProKey(rawKeyInput: String): Result<ProLicenseKeyEntity> = runCatching {
        val key = rawKeyInput.trim().uppercase(Locale.US)
        require(KEY_PATTERN.matches(key)) { "Enter a valid AYAAN Pro Key." }
        val firebaseUser = auth.currentUser ?: error("Sign in before activating a Pro key.")
        val profileReference = firestore.collection("users").document(firebaseUser.uid)
        val keyReference = firestore.collection(KEY_COLLECTION).document(key)
        val localUser = userDao.getActiveSession()
            ?: error("The active account is unavailable. Sign in again.")
        check(localUser.firebaseUid == firebaseUser.uid && !localUser.isRemoved) {
            "The active account could not be verified."
        }

        val activated = firestore.runTransaction { transaction ->
            val keySnapshot = transaction.get(keyReference)
            if (!keySnapshot.exists()) error(INVALID_KEY_MESSAGE)
            if (keySnapshot.getBoolean("isUsed") == true) error(USED_KEY_MESSAGE)

            val plan = keySnapshot.getString("planTier") ?: error("This Pro key has no valid plan.")
            val durationDays = (keySnapshot.getLong("durationDays") ?: 0L).toInt()
            val price = (keySnapshot.getLong("pricePkr") ?: 0L).toInt()
            require(ProPlanTier.values().any { it.tierId == plan } && durationDays in setOf(7, 30, 365)) {
                "This Pro key has invalid plan data."
            }
            val profile = transaction.get(profileReference)
            check(profile.exists() && profile.getBoolean("isRemoved") != true) {
                "This account cannot activate a Pro key."
            }
            val now = System.currentTimeMillis()
            val expiresAt = now + durationDays * DAY_MILLIS
            transaction.update(
                keyReference,
                mapOf(
                    "isUsed" to true,
                    "usedByUid" to firebaseUser.uid,
                    "usedByUsername" to localUser.username,
                    "usedByEmail" to (firebaseUser.email ?: localUser.email),
                    "activatedAt" to now,
                    "expiresAt" to expiresAt
                )
            )
            transaction.update(
                profileReference,
                mapOf(
                    "subscriptionPlan" to plan,
                    "proExpiresAt" to expiresAt,
                    "activatedProKey" to key
                )
            )
            ProLicenseKeyEntity(
                licenseKey = key,
                planTier = plan,
                durationDays = durationDays,
                pricePkr = price,
                isUsed = true,
                usedByUsername = localUser.username,
                usedByEmail = firebaseUser.email ?: localUser.email,
                usedByUid = firebaseUser.uid,
                createdAt = keySnapshot.getLong("createdAt") ?: 0L,
                activatedAt = now,
                expiresAt = expiresAt,
                aiNote = keySnapshot.getString("aiNote").orEmpty()
            )
        }.await()

        val existing = keyDao.getProKeyByCode(key)
        if (existing == null) keyDao.insertProKeys(listOf(activated))
        val changedRows = keyDao.markKeyUsedAtomically(
            key = key,
            username = localUser.username,
            email = firebaseUser.email ?: localUser.email,
            uid = firebaseUser.uid,
            activatedAt = activated.activatedAt,
            expiresAt = activated.expiresAt
        )
        if (changedRows == 0) {
            keyDao.insertProKeys(listOf(activated))
        }
        userDao.updateUserProSubscriptionByFirebaseUid(
            firebaseUid = firebaseUser.uid,
            plan = activated.planTier,
            expiresAt = activated.expiresAt,
            licenseKey = key
        )
        mutableProStatus.value = activated.toProStatus()
        activated
    }.recoverCatching { error ->
        if (error.message?.contains("already been used", ignoreCase = true) == true) {
            throw IllegalStateException(USED_KEY_MESSAGE, error)
        }
        throw error
    }

    suspend fun deleteProKey(key: String) {
        requireAdmin()
        val snapshot = firestore.collection(KEY_COLLECTION).document(key).get().await()
        check(snapshot.getBoolean("isUsed") != true) { "Redeemed keys cannot be deleted." }
        firestore.collection(KEY_COLLECTION).document(key).delete().await()
        keyDao.deleteProKey(key)
    }

    suspend fun refreshCurrentProStatus() {
        val user = userDao.getActiveSession()
        if (user != null && user.firebaseUid == auth.currentUser?.uid) {
            val profile = firestore.collection("users").document(user.firebaseUid).get().await()
            val plan = profile.getString("subscriptionPlan") ?: "FREE"
            val expiresAt = profile.getLong("proExpiresAt") ?: 0L
            val key = profile.getString("activatedProKey").orEmpty()
            userDao.updateUserProSubscriptionByFirebaseUid(user.firebaseUid, plan, expiresAt, key)
            mutableProStatus.value = user.copy(
                subscriptionPlan = plan,
                proExpiresAt = expiresAt,
                activatedProKey = key
            ).toProStatus(hasAuthorizedAdminClaim())
        }
    }

    private suspend fun createUniqueKey(tier: ProPlanTier): String {
        repeat(12) {
            val candidate = "AYAAN-${tier.keyPrefix}-${randomGroup()}-${randomGroup()}-${randomGroup()}"
            if (keyDao.getProKeyByCode(candidate) == null &&
                !firestore.collection(KEY_COLLECTION).document(candidate).get().await().exists()
            ) return candidate
        }
        error("Unable to create a unique Pro key. Please retry.")
    }

    private fun randomGroup(): String = buildString(4) {
        repeat(4) { append(KEY_ALPHABET[random.nextInt(KEY_ALPHABET.length)]) }
    }

    private suspend fun requireAdmin() {
        check(AdminRepository(appContext).isAdminSession()) {
            "Pro keys can only be generated or revoked by the verified administrator."
        }
    }

    private suspend fun hasAuthorizedAdminClaim(): Boolean =
        runCatching { AdminRepository(appContext).isAdminSession() }.getOrDefault(false)

    private fun UserEntity?.toProStatus(admin: Boolean): ProStatusState {
        val now = System.currentTimeMillis()
        val active = admin || (this?.proExpiresAt ?: 0L) > now
        val planName = when {
            admin -> "Admin Unlimited"
            !active -> "Free Plan"
            this?.subscriptionPlan == ProPlanTier.WEEKLY.tierId -> "Weekly Pro"
            this?.subscriptionPlan == ProPlanTier.MONTHLY.tierId -> "Monthly Pro"
            this?.subscriptionPlan == ProPlanTier.YEARLY.tierId -> "Yearly Pro"
            else -> "Pro Plan"
        }
        val expiresAt = this?.proExpiresAt ?: 0L
        return ProStatusState(
            isProActive = active,
            activePlanName = planName,
            proExpiresAt = expiresAt,
            remainingDays = if (expiresAt > now) ((expiresAt - now + DAY_MILLIS - 1) / DAY_MILLIS).toInt() else 0,
            activatedKey = this?.activatedProKey.orEmpty()
        )
    }

    private fun ProLicenseKeyEntity.toProStatus(): ProStatusState =
        ProStatusState(
            isProActive = expiresAt > System.currentTimeMillis(),
            activePlanName = when (planTier) {
                ProPlanTier.WEEKLY.tierId -> "Weekly Pro"
                ProPlanTier.MONTHLY.tierId -> "Monthly Pro"
                ProPlanTier.YEARLY.tierId -> "Yearly Pro"
                else -> "Pro Plan"
            },
            proExpiresAt = expiresAt,
            remainingDays = ((expiresAt - System.currentTimeMillis() + DAY_MILLIS - 1) / DAY_MILLIS)
                .toInt().coerceAtLeast(0),
            activatedKey = licenseKey
        )

    private fun ProLicenseKeyEntity.toFirestore(): Map<String, Any> = mapOf(
        "licenseKey" to licenseKey,
        "planTier" to planTier,
        "durationDays" to durationDays,
        "pricePkr" to pricePkr,
        "isUsed" to isUsed,
        "usedByUsername" to usedByUsername,
        "usedByEmail" to usedByEmail,
        "usedByUid" to usedByUid,
        "createdAt" to createdAt,
        "activatedAt" to activatedAt,
        "expiresAt" to expiresAt,
        "aiNote" to aiNote
    )

    companion object {
        private const val TAG = "ProSubscription"
        private const val KEY_COLLECTION = "pro_license_keys"
        private const val GROQ_CHAT_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val groqHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(18, TimeUnit.SECONDS)
            .build()
        private const val KEY_USAGE_PREFIX = "usage_"
        private const val KEY_USAGE_DAY_PREFIX = "usage_day_"
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        private val INVALID_KEY_MESSAGE =
            "Invalid Pro Key. Contact ${BuildConfig.PRO_SUPPORT_EMAIL} to buy a valid Pro Key. Email support: ${BuildConfig.GMAIL_SENDER_EMAIL}."
        private const val USED_KEY_MESSAGE = "This Pro Key has already been used and cannot be used again!"
        private val KEY_PATTERN = Regex("^AYAAN-(WEEK|MNTH|YEAR)-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$")
        private val KEY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()
        @Volatile private var instance: ProSubscriptionRepository? = null

        fun getInstance(context: Context): ProSubscriptionRepository =
            instance ?: synchronized(this) {
                instance ?: ProSubscriptionRepository(context).also { instance = it }
            }
    }
}

