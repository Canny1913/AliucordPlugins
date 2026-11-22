package com.github.canny1913.entries

import com.discord.api.user.User
import com.discord.models.message.Message
import com.discord.widgets.chat.list.entries.ChatListEntry

abstract class AversionEntry(
    val aversionCount: Int,
    val isExpanded: Boolean,
    val authors: List<User>,
    val message: Message,
    val hidden: Boolean,
    @JvmField
    val key: String,
    @JvmField
    val type: Int
): ChatListEntry() {
    override fun getKey(): String? = key
    override fun getType(): Int = type
    override fun isInExpandedBlockedMessageChunk(): Boolean = isExpanded
}