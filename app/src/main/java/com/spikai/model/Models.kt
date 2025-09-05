package com.spikai.model

import kotlinx.serialization.Serializable
import java.util.UUID

// MARK: - ConnectionStatus
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}

// MARK: - ConversationItem
@Serializable
data class ConversationItem(
    val id: String,
    val role: String,
    var text: String = ""
) {
    constructor(role: String, text: String = "") : this(
        id = UUID.randomUUID().toString(),
        role = role,
        text = text
    )
}
