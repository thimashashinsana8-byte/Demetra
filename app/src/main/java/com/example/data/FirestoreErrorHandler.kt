package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    GET,
    LIST,
    WRITE
}

object FirestoreErrorHandler {
    private const val TAG = "FirestoreError"

    fun handleFirestoreError(
        error: Exception,
        operationType: OperationType,
        path: String?
    ) {
        val currentUser = try {
            Firebase.auth.currentUser
        } catch (_: Exception) {
            null
        }

        val payload = JSONObject().apply {
            put("error", error.message ?: error.toString())
            put("operationType", operationType.name)
            put("path", path ?: JSONObject.NULL)
            put(
                "authInfo",
                JSONObject().apply {
                    put("userId", currentUser?.uid ?: JSONObject.NULL)
                    put("email", currentUser?.email ?: JSONObject.NULL)
                    put("emailVerified", currentUser?.isEmailVerified ?: false)
                    put(
                        "providerInfo",
                        JSONArray().apply {
                            currentUser?.providerData?.forEach { info ->
                                put(
                                    JSONObject().apply {
                                        put("providerId", info.providerId)
                                        put("displayName", info.displayName ?: JSONObject.NULL)
                                        put("email", info.email ?: JSONObject.NULL)
                                    }
                                )
                            }
                        }
                    )
                }
            )
        }
        Log.e(TAG, payload.toString(), error)
    }
}
