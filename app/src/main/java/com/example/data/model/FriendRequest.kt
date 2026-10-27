package com.example.data.model

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderDisplayName: String = "",
    val senderAvatarColorHex: String = "#8B5CF6",
    val receiverId: String = "",
    val receiverUsername: String = "",
    val status: String = STATUS_PENDING,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ACCEPTED = "ACCEPTED"
        const val STATUS_DECLINED = "DECLINED"
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "senderId" to senderId,
            "senderUsername" to senderUsername,
            "senderDisplayName" to senderDisplayName,
            "senderAvatarColorHex" to senderAvatarColorHex,
            "receiverId" to receiverId,
            "receiverUsername" to receiverUsername,
            "status" to status,
            "timestamp" to timestamp
        )
    }
}
