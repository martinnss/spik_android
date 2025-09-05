package com.spikai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - Conversation Flow Analysis Models
@Serializable
data class ConversationFlowAnalysis(
    @SerialName("continue")
    val shouldContinue: Boolean,
    val reason: String,
    val confidence: Double? = 1.0
)

// MARK: - Level Progression Evaluation Models
@Serializable
data class LevelProgressionEvaluation(
    val score: Int,
    @SerialName("ready_for_next_level")
    val passed: Boolean,
    @SerialName("feedback")
    val reasoning: String,
    @SerialName("next_level_recommendation")
    val nextLevelRecommendation: String = "",
    @SerialName("areas_to_improve")
    val areasToImprove: List<String> = emptyList(),
    val strengths: List<String> = emptyList()
) {
    companion object {
        // MARK: - Preview Helper
        fun preview(): LevelProgressionEvaluation {
            return LevelProgressionEvaluation(
                score = 4,
                passed = true,
                reasoning = "Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.Has demostrado un buen dominio del vocabulario básico y puedes mantener conversaciones simples con confianza.",
                nextLevelRecommendation = "Estás listo para avanzar al siguiente nivel donde practicarás expresiones más complejas.",
                areasToImprove = listOf("Pronunciación de sonidos específicos", "Uso de tiempos verbales"),
                strengths = listOf("Vocabulario básico sólido", "Buena comprensión auditiva", "Confianza al hablar")
            )
        }
    }
}

// MARK: - Request Models
@Serializable
data class ConversationAnalysisRequest(
    val conversation: List<ConversationMessage>,
    @SerialName("level_id")
    val levelId: Int
)

@Serializable
data class LevelProgressionRequest(
    val conversation: List<ConversationMessage>,
    @SerialName("current_level_id")
    val currentLevelId: Int
)

@Serializable
data class ConversationMessage(
    val role: String,
    val content: String
)

// MARK: - Extension to convert ConversationItem to ConversationMessage
fun ConversationItem.toConversationMessage(): ConversationMessage {
    val role = if (this.role == "user") "user" else "assistant"
    return ConversationMessage(role = role, content = text)
}
