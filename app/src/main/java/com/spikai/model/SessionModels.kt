package com.spikai.model

import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

// MARK: - UserSession Model
@Serializable
data class UserSession(
    val userId: String,
    val sessionId: String,
    val startTime: @Serializable(with = DateSerializer::class) Date,
    var endTime: @Serializable(with = DateSerializer::class) Date? = null,
    var levels: List<SessionLevel> = emptyList(),
    val deviceInfo: DeviceInfo
) {
    constructor(userId: String) : this(
        userId = userId,
        sessionId = UUID.randomUUID().toString(),
        startTime = Date(),
        endTime = null,
        levels = emptyList(),
        deviceInfo = DeviceInfo()
    )
}

// MARK: - SessionLevel Model
@Serializable
data class SessionLevel(
    val levelId: Int,
    var tries: Int = 0,
    var completed: Boolean = false,
    val startTime: @Serializable(with = DateSerializer::class) Date,
    var endTime: @Serializable(with = DateSerializer::class) Date? = null
) {
    constructor(levelId: Int) : this(
        levelId = levelId,
        tries = 0,
        completed = false,
        startTime = Date(),
        endTime = null
    )
}

// MARK: - DeviceInfo Model
@Serializable
data class DeviceInfo(
    val platform: String = "Android",
    val appVersion: String = "1.0" // Default value, should be set with actual version
) {
    companion object {
        fun create(context: android.content.Context): DeviceInfo {
            return DeviceInfo(
                platform = "Android",
                appVersion = getAppVersion(context)
            )
        }
    }
}

// Helper function to get app version
private fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0" // Fallback version
    }
}

// MARK: - Date Serializer for Kotlinx Serialization
object DateSerializer : kotlinx.serialization.KSerializer<Date> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("Date", kotlinx.serialization.descriptors.PrimitiveKind.LONG)
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Date) {
        encoder.encodeLong(value.time)
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Date {
        return Date(decoder.decodeLong())
    }
}

// MARK: - Extensions for backend compatibility will be in SessionReportingService
