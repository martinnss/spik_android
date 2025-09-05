package com.spikai.model

import kotlinx.serialization.Serializable
import java.util.UUID

// MARK: - Assessment Questions
enum class AssessmentQuestion(val value: Int) {
    SPEAKING(1),
    LISTENING(2),
    CAPABILITY(3),
    MISTAKES(4),
    GOALS(5);

    val question: String
        get() = when (this) {
            SPEAKING -> "¿Qué tan cómodo/a te sientes al hablar en inglés con alguien?"
            LISTENING -> "¿Qué entiendes cuando escuchas inglés hablado en persona o en videos?"
            CAPABILITY -> "¿Qué podrías hacer hoy en inglés sin ayuda?"
            MISTAKES -> "¿Qué errores sueles cometer al hablar inglés?"
            GOALS -> "¿Qué te gustaría lograr con tu inglés?"
        }

    val options: List<AssessmentOption>
        get() = when (this) {
            SPEAKING -> listOf(
                AssessmentOption(iconName = "face.dashed", title = "Nervioso", description = "Me paralizo, no sé qué decir", level = 1),
                AssessmentOption(iconName = "bubble.left", title = "Básico", description = "Puedo decir frases básicas", level = 2),
                AssessmentOption(iconName = "person.2", title = "Conversacional", description = "Puedo conversar", level = 3),
                AssessmentOption(iconName = "star", title = "Fluido", description = "Hablo con fluidez y confianza", level = 4)
            )
            LISTENING -> listOf(
                AssessmentOption(iconName = "xmark.circle", title = "Casi nada", description = "Necesito traducción", level = 1),
                AssessmentOption(iconName = "ear", title = "Palabras básicas", description = "Entiendo frases simples", level = 2),
                AssessmentOption(iconName = "ear.trianglebadge.exclamationmark", title = "Conversación clara", description = "La mayoría si hablan claro", level = 3),
                AssessmentOption(iconName = "waveform", title = "Todo tipo", description = "Entiendo perfectamente", level = 4)
            )
            CAPABILITY -> listOf(
                AssessmentOption(iconName = "hand.wave", title = "Saludos básicos", description = "Saludar y pedir comida", level = 1),
                AssessmentOption(iconName = "questionmark.bubble", title = "Información personal", description = "Hacer preguntas y dar info", level = 2),
                AssessmentOption(iconName = "text.bubble", title = "Historias simples", description = "Contar una historia simple", level = 3),
                AssessmentOption(iconName = "brain.head.profile", title = "Ideas complejas", description = "Ej: Hablar sobre emociones", level = 4)
            )
            MISTAKES -> listOf(
                AssessmentOption(iconName = "exclamationmark.triangle", title = "Muchos errores", description = "No sé formar frases", level = 1),
                AssessmentOption(iconName = "pause.circle", title = "Pausas frecuentes", description = "Me trabo mucho", level = 2),
                AssessmentOption(iconName = "clock", title = "Errores de tiempo", description = "Uso mal los tiempos verbales", level = 3),
                AssessmentOption(iconName = "checkmark.circle", title = "Casi perfecto", description = "Cometo errores mínimos", level = 4)
            )
            GOALS -> listOf(
                AssessmentOption(iconName = "airplane", title = "Supervivencia", description = "Sobrevivir en viajes", level = 1),
                AssessmentOption(iconName = "message", title = "Confianza diaria", description = "Hablar con más confianza", level = 2),
                AssessmentOption(iconName = "briefcase", title = "Trabajo/Estudios", description = "Comunicarme mejor", level = 3),
                AssessmentOption(iconName = "crown", title = "Nivel nativo", description = "Sonar como nativo", level = 4)
            )
        }

    companion object {
        fun fromInt(value: Int): AssessmentQuestion? {
            return values().find { it.value == value }
        }
        
        val allCases: List<AssessmentQuestion> = values().toList()
    }
}

@Serializable
data class AssessmentOption(
    val iconName: String,
    val title: String,
    val description: String,
    val level: Int
) {
    @kotlinx.serialization.Transient
    val id: String = UUID.randomUUID().toString()
}

// MARK: - English Level (calculated from assessment)
enum class EnglishLevel(val rawValue: String) {
    PRINCIPIANTE("Principiante"),
    BASICO("Básico"),
    INTERMEDIO("Intermedio"),
    AVANZADO("Avanzado");

    val id: String get() = rawValue

    val description: String
        get() = when (this) {
            PRINCIPIANTE -> "Empezando tu viaje en inglés"
            BASICO -> "Puedes manejar situaciones básicas"
            INTERMEDIO -> "Puedes mantener conversaciones cotidianas"
            AVANZADO -> "Hablas con confianza en la mayoría de situaciones"
        }

    val baseLevelId: Int
        get() = when (this) {
            PRINCIPIANTE -> 1000
            BASICO -> 2000
            INTERMEDIO -> 3000
            AVANZADO -> 4000
        }

    companion object {
        fun fromAverageScore(score: Double): EnglishLevel {
            return when {
                score >= 1.0 && score < 2.5 -> PRINCIPIANTE
                score >= 2.5 && score < 3.5 -> BASICO
                score >= 3.5 && score <= 4.0 -> INTERMEDIO
                else -> AVANZADO
            }
        }
        
        val allCases: List<EnglishLevel> = values().toList()
    }
}

// MARK: - User Profile
@Serializable
data class UserProfile(
    var name: String = "",
    var assessmentAnswers: Map<Int, Int> = emptyMap(), // stores the level (1-4) for each question by question value
    var englishLevel: EnglishLevel? = null,
    var hasCompletedOnboarding: Boolean = false
) {
    val levelId: Int?
        get() = englishLevel?.baseLevelId

    val averageAssessmentScore: Double
        get() {
            if (assessmentAnswers.isEmpty()) return 1.0
            val sum = assessmentAnswers.values.sum()
            return sum.toDouble() / assessmentAnswers.size.toDouble()
        }

    fun calculateEnglishLevel(): UserProfile {
        return this.copy(englishLevel = EnglishLevel.fromAverageScore(averageAssessmentScore))
    }

    val levelDisplayText: String
        get() {
            return when (englishLevel) {
                EnglishLevel.PRINCIPIANTE -> "A1 - Principiante"
                EnglishLevel.BASICO -> "A2 - Básico"
                EnglishLevel.INTERMEDIO -> "B1 - Intermedio"
                EnglishLevel.AVANZADO -> "B2/C1 - Avanzado"
                null -> "Nivel no determinado"
            }
        }
}
