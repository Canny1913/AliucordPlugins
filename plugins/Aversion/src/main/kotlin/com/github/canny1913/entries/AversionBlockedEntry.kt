package com.github.canny1913.entries

import com.discord.api.user.User
import com.discord.models.message.Message

data class AversionBlockedEntry(
    val blockedCount: Int,
    val isExpandedBlock: Boolean,
    val blockedAuthors: List<User>,
    val adapterMessage: Message,
    val isHidden: Boolean
): AversionEntry(
    aversionCount = blockedCount,
    isExpanded = isExpandedBlock,
    authors = blockedAuthors,
    message = adapterMessage,
    key = adapterMessage.id.toString(),
    type = TYPE,
    hidden = isHidden
) {
    companion object {
        const val TYPE = 68
    }
}