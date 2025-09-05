package com.spikai.service

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// MARK: - FirestoreService
class FirestoreService {
    
    val db: FirebaseFirestore
    private val errorHandler = ErrorHandlingService.shared
    private val json = Json { ignoreUnknownKeys = true }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        val shared = FirestoreService()
    }
    
    init {
        this.db = Firebase.firestore
    }
    
    // MARK: - Generic Methods
    
    /// Save data to any collection
    suspend fun <T> saveData(
        collection: String,
        document: String? = null,
        data: T,
        merge: Boolean = false
    ): Result<String> where T : Any {
        return withContext(Dispatchers.IO) {
            try {
                val documentRef = if (document != null) {
                    db.collection(collection).document(document)
                } else {
                    db.collection(collection).document()
                }

                if (merge) {
                    documentRef.set(data, SetOptions.merge()).await()
                } else {
                    documentRef.set(data).await()
                }

                Result.success(documentRef.id)
            } catch (error: Exception) {
                val spikError = errorHandler.handleError(error)
                errorHandler.showError(spikError)
                Result.failure(Exception(spikError.errorDescription))
            }
        }
    }
    
    /// Save data using dictionary (for more control)
    suspend fun saveDataDictionary(
        collection: String,
        document: String? = null,
        data: Map<String, Any>,
        merge: Boolean = false
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val documentRef = if (document != null) {
                    db.collection(collection).document(document)
                } else {
                    db.collection(collection).document()
                }
                
                if (merge) {
                    documentRef.set(data, SetOptions.merge()).await()
                } else {
                    documentRef.set(data).await()
                }
                
                Result.success(documentRef.id)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
    }
    
    /// Update specific fields in a document
    suspend fun updateData(
        collection: String,
        document: String,
        fields: Map<String, Any>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.collection(collection).document(document)
                    .update(fields)
                    .await()
                
                Result.success(Unit)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
    }
    
    /// Get document from collection
    suspend inline fun <reified T> getData(
        collection: String,
        document: String
    ): Result<T> where T : Any {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = db.collection(collection).document(document)
                    .get()
                    .await()
                
                if (!snapshot.exists()) {
                    return@withContext Result.failure(FirestoreError.DocumentNotFound)
                }
                
                val data = snapshot.toObject(T::class.java)
                if (data == null) {
                    return@withContext Result.failure(FirestoreError.InvalidData)
                }
                
                Result.success(data)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
    }
}

// MARK: - Custom Errors
sealed class FirestoreError(message: String) : Exception(message) {
    object DocumentNotFound : FirestoreError("Document not found")
    object InvalidData : FirestoreError("Invalid data format")
}
