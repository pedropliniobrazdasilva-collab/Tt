package com.example.model

import java.util.UUID

enum class MapStatus {
    AVAILABLE,
    BANNED,
    PICKED
}

data class MapItem(
    val id: String,
    val name: String,
    val status: MapStatus = MapStatus.AVAILABLE,
    val actionedBy: String? = null // "Capitão A", "Capitão B" or "Bot"
)

data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rating: Int = (10..20).random(), // Game rating (high rating = higher skill)
    val isCaptain: Boolean = false,
    val team: String? = null // "A", "B"
)

enum class MessageType {
    INFO,
    COMMAND, // e.g. "/md1 auto"
    SUCCESS,
    SYSTEM
}

data class BotMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: MessageType = MessageType.INFO,
    val timestamp: String
)

enum class MatchFormat {
    MD1,
    MD3,
    MD5
}

enum class BotState {
    IDLE,
    QUEUE_WAITING,
    CAPTAINS_SELECTING,
    MAP_VETO,
    TEAM_DIVIDING,
    FINISHED
}
