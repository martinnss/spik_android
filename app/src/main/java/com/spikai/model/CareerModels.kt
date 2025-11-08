package com.spikai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// MARK: - Career Level
@Serializable
data class CareerLevel(
    @SerialName("level_id")
    val levelId: Int,
    val title: String,
    val description: String,
    @SerialName("to_learn")
    val toLearn: List<String>,
    @SerialName("ios_symbol")
    val iosSymbol: String,
    @SerialName("is_unlocked")
    var isUnlocked: Boolean = false,
    @SerialName("is_completed")
    var isCompleted: Boolean = false,
    val experience: Int = 0,
    @SerialName("total_experience")
    val totalExperience: Int = 100
) {
    // Non-serializable properties
    @kotlinx.serialization.Transient
    val id: String = UUID.randomUUID().toString()
    
    // Computed properties for backward compatibility
    val number: Int get() = levelId
    val skills: List<String> get() = toLearn
    
    val progress: Double
        get() = if (totalExperience > 0) {
            experience.toDouble() / totalExperience.toDouble()
        } else {
            0.0
        }
    

}

// MARK: - Career Progress
@Serializable
data class CareerProgress(
    var currentLevel: Int = 1001, // Default to principiante level
    var totalExperience: Int = 0,
    var completedLevels: Set<Int> = emptySet(),
    var unlockedLevels: Set<Int> = setOf(1001) // Unlock first level by default
) {
    constructor(basedOnEnglishLevel: EnglishLevel) : this() {
        val baseLevelId = basedOnEnglishLevel.baseLevelId
        val firstLevelId = baseLevelId + 1 // First level for the user's proficiency
        
        this.currentLevel = firstLevelId
        this.totalExperience = 0
        this.completedLevels = emptySet()
        this.unlockedLevels = setOf(firstLevelId) // Unlock first appropriate level
    }
}

// MARK: - API Response Models
@Serializable
data class LevelsResponse(
    val levels: List<CareerLevel>
)

// MARK: - Sample Data for Levels (Deprecated - will be removed)
object CareerLevelSamples {
    val sampleLevels: List<CareerLevel> = listOf(
        // Principiante levels (1000+)
        CareerLevel(
            levelId = 1001,
            title = "Saludos Básicos",
            description = "Aprende a saludar y presentarte en inglés",
            toLearn = listOf("Hello, Hi, Good morning", "My name is...", "Nice to meet you"),
            iosSymbol = "hand.wave.fill",
            isUnlocked = true,
            isCompleted = false,
            experience = 0,
            totalExperience = 100
        ),
        CareerLevel(
            levelId = 1002,
            title = "Números y Colores",
            description = "Vocabulario básico de números y colores",
            toLearn = listOf("1-20", "Red, blue, green", "How many?"),
            iosSymbol = "123.rectangle.fill",
            isUnlocked = false,
            isCompleted = false,
            experience = 0,
            totalExperience = 100
        ),
        // Básico levels (2000+)
        CareerLevel(
            levelId = 2001,
            title = "Información Personal",
            description = "Habla sobre ti mismo y tu familia",
            toLearn = listOf("Where are you from?", "Family members", "Age and occupation"),
            iosSymbol = "person.2.fill",
            isUnlocked = false,
            isCompleted = false,
            experience = 0,
            totalExperience = 150
        ),
        // Intermedio levels (3000+)
        CareerLevel(
            levelId = 3001,
            title = "Expresar Opiniones",
            description = "Aprende a dar tu opinión sobre diferentes temas",
            toLearn = listOf("I think that...", "In my opinion", "I agree/disagree"),
            iosSymbol = "bubble.left.and.bubble.right.fill",
            isUnlocked = false,
            isCompleted = false,
            experience = 0,
            totalExperience = 200
        ),
        // Avanzado levels (4000+)
        CareerLevel(
            levelId = 4001,
            title = "Debates Complejos",
            description = "Participa en debates y discusiones avanzadas",
            toLearn = listOf("Argumentative structures", "Complex grammar", "Idiomatic expressions"),
            iosSymbol = "person.3.sequence.fill",
            isUnlocked = false,
            isCompleted = false,
            experience = 0,
            totalExperience = 250
        )
    )
}
