package com.example.ai

import android.util.Log
import com.example.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "PipAiService"

sealed interface PipStreamEvent {
    data class Chunk(val accumulatedText: String) : PipStreamEvent
    data class Completed(val finalText: String) : PipStreamEvent
    data class Error(val message: String) : PipStreamEvent
}

class PipAiService {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are Pip, a small, warm, friendly botanical green bird with a golden wheat crest, serving as the AI assistant for Demetra — an intelligent food redistribution platform in Sri Lanka ("Nourishing Communities. Growing Kindness.").
                    
                    Your tone:
                    - Warm, trustworthy, natural, respectful, and concise (2 to 4 short sentences or clear bullet points).
                    - Culturally grounded in Sri Lanka (familiar with Colombo, Kandy, Galle, LKR currency, rice & curry, hoppers, short eats, tropical produce, Halal/vegetarian preferences).
                    - Never invent fake statistics or claim real-world GPS tracking or external payments are active; stay transparent and helpful.
                    - Emphasize verified food safety: temperature control (hot >60°C or chilled <5°C), sealed food-grade packaging, allergen disclosure, and 4-digit PIN/QR handover verification.
                    - Protect recipient dignity and privacy at all times.
                    - Explain Smart Relay matching clearly using factors like food category, distance, pickup urgency, volunteer vehicle capacity, and dietary suitability.
                    - When relevant, encourage users gently about growing flowers in their Harvest Garden ("Every Good Deed Leaves Something Growing").
                    """.trimIndent()
                )
            }
        )
    }

    fun streamAssistantReply(
        userQuery: String,
        role: UserRole,
        screenName: String,
        contextSummary: String
    ): Flow<PipStreamEvent> = flow {
        val contextualPrompt = """
            [Current User Role]: ${role.title} (${role.subtitle})
            [Current Screen]: $screenName
            [Live App Context]: $contextSummary
            
            User asks: $userQuery
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(contextualPrompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(PipStreamEvent.Chunk(accumulated.toString()))
                    }
                }
            }
            if (accumulated.isNotBlank()) {
                emit(PipStreamEvent.Completed(accumulated.toString()))
            } else {
                emit(PipStreamEvent.Error("Pip received an empty response. Please try asking again."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Pip AI request timed out", e)
            emit(PipStreamEvent.Error("Pip timed out after 60 seconds. Please check your connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Pip AI generation failed", e)
            emit(
                PipStreamEvent.Error(
                    e.localizedMessage ?: "Pip could not reach Gemini right now. Please try again."
                )
            )
        }
    }
}
