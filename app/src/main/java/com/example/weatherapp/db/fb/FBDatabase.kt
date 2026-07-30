package com.example.weatherapp.db.fb

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FBDatabase {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    val user: Flow<FBUser>
        get() {
            val currentUser = auth.currentUser ?: return emptyFlow()
            return callbackFlow {
                trySend(fallbackUser(currentUser))
                val registration = db.collection("users")
                    .document(currentUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(fallbackUser(currentUser))
                            return@addSnapshotListener
                        }
                        val stored = snapshot?.toObject(FBUser::class.java)
                        val fbUser = when {
                            snapshot == null || !snapshot.exists() ||
                                stored == null || stored.name.isNullOrBlank() -> {
                                fallbackUser(currentUser).apply {
                                    stored?.email?.takeIf { it.isNotBlank() }?.let { email = it }
                                }
                            }
                            else -> stored
                        }
                        trySend(fbUser)
                    }
                awaitClose { registration.remove() }
            }
        }

    val cities: Flow<List<FBCity>>
        get() {
            val uid = auth.currentUser?.uid ?: return emptyFlow()
            return db.collection("users")
                .document(uid)
                .collection("cities")
                .snapshots().map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(FBCity::class.java)?.apply {
                            if (name.isNullOrBlank()) name = doc.id
                        }
                    }
                }
        }

    fun register(user: FBUser) {
        if (auth.currentUser == null)
            throw RuntimeException("User not logged in!")
        val uid = auth.currentUser!!.uid
        db.collection("users").document(uid).set(user)
    }

    fun ensureUserDocument() {
        val currentUser = auth.currentUser ?: return
        val ref = db.collection("users").document(currentUser.uid)
        ref.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                ref.set(userMap(currentUser), SetOptions.merge())
            }
        }
    }

    suspend fun ensureUserDocumentSync() {
        val currentUser = auth.currentUser ?: return
        try {
            currentUser.getIdToken(true).await()
            val ref = db.collection("users").document(currentUser.uid)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) {
                ref.set(userMap(currentUser), SetOptions.merge()).await()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun add(city: FBCity): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Usuário não logado"))
        if (city.name.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Nome da cidade vazio"))
        }
        return try {
            currentUser.getIdToken(true).await()
            ensureUserDocumentSync()
            val uid = currentUser.uid
            val docId = cityDocumentId(city.name!!)
            db.collection("users").document(uid).collection("cities")
                .document(docId)
                .set(cityMap(city), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun remove(city: FBCity) {
        if (auth.currentUser == null)
            throw RuntimeException("User not logged in!")
        if (city.name == null || city.name!!.isEmpty())
            throw RuntimeException("City with null or empty name!")
        val uid = auth.currentUser!!.uid
        db.collection("users").document(uid).collection("cities")
            .document(cityDocumentId(city.name!!)).delete()
    }

    fun update(city: FBCity) {
        if (auth.currentUser == null) throw RuntimeException("Not logged in!")
        val uid = auth.currentUser!!.uid
        val changes = mapOf(
            "lat" to city.lat,
            "lng" to city.lng,
            "monitored" to city.monitored
        )
        db.collection("users").document(uid)
            .collection("cities").document(cityDocumentId(city.name!!)).update(changes)
    }

    private fun userMap(currentUser: FirebaseUser): Map<String, Any?> = mapOf(
        "name" to (
            currentUser.displayName
                ?: currentUser.email?.substringBefore('@')
                ?: "Usuário"
            ),
        "email" to currentUser.email
    )

    private fun cityMap(city: FBCity): Map<String, Any?> = mapOf(
        "name" to city.name,
        "lat" to city.lat,
        "lng" to city.lng,
        "monitored" to city.monitored
    )

    private fun cityDocumentId(name: String): String =
        name.replace("/", "-").take(1500)

    private fun fallbackUser(currentUser: FirebaseUser): FBUser {
        return FBUser().apply {
            name = currentUser.displayName
                ?: currentUser.email?.substringBefore('@')
                ?: "Usuário"
            email = currentUser.email
        }
    }
}
