package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DuaItem(
    val id: String = "",
    val userName: String = "Anonymous Believer",
    val userLocation: String = "Ummah",
    val content: String = "",
    val category: String = "General",
    val ameenCount: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * MODULE 14: GLOBAL DUA WALL & REAL-TIME HAPTIC UMMAH (FIRESTORE REPOSITORY)
 */
class FirestoreRepository(
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Throwable) {
        Log.w("FirestoreRepository", "Firebase not initialized, using local fallback: ${e.message}")
        null
    }
) {
    companion object {
        private const val TAG = "FirestoreRepository"
        const val COLLECTION_PRAYER_REQUESTS = "prayer_requests"
    }

    /**
     * Real-time stream of prayer requests ordered by timestamp descending
     */
    fun getPrayerRequestsFlow(): Flow<List<DuaItem>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        try {
            val listenerRegistration = db.collection(COLLECTION_PRAYER_REQUESTS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore listen failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            try {
                                DuaItem(
                                    id = doc.id,
                                    userName = doc.getString("userName") ?: "Believer",
                                    userLocation = doc.getString("userLocation") ?: "Ummah",
                                    content = doc.getString("content") ?: "",
                                    category = doc.getString("category") ?: "General",
                                    ameenCount = doc.getLong("ameenCount") ?: 0L,
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(items)
                    }
                }
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Firestore snapshotFlow", e)
            close(e)
        }
    }

    /**
     * Increments the Ameen counter using FieldValue.increment(1)
     */
    fun incrementAmeen(duaId: String) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_PRAYER_REQUESTS)
                .document(duaId)
                .update("ameenCount", FieldValue.increment(1))
                .addOnSuccessListener {
                    Log.d(TAG, "Ameen incremented for $duaId")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to increment Ameen on Firestore", e)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore update exception", e)
        }
    }

    /**
     * Submits a new Dua to the global stream
     */
    fun submitDua(dua: DuaItem, onComplete: (Boolean) -> Unit = {}) {
        val db = firestore
        if (db == null) {
            onComplete(true)
            return
        }
        try {
            val data = hashMapOf(
                "userName" to dua.userName,
                "userLocation" to dua.userLocation,
                "content" to dua.content,
                "category" to dua.category,
                "ameenCount" to 0L,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_PRAYER_REQUESTS)
                .add(data)
                .addOnSuccessListener {
                    Log.d(TAG, "Dua submitted successfully with id: ${it.id}")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed submitting Dua to Firestore", e)
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore add exception", e)
            onComplete(false)
        }
    }
}
