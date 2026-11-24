package com.github.canny1913.entries

import com.discord.api.user.User

data class AversionBlockedEntry(
    val blockedCount: Int,
    val isExpandedBlock: Boolean,
    val blockedAuthors: List<User>,
    val messages: Set<Long>,
    val isHidden: Boolean
): AversionEntry(
    aversionCount = blockedCount,
    authors = blockedAuthors,
    isExpanded = isExpandedBlock,
    messageIds = messages,
    key = messages.firstOrNull().toString(),
    type = TYPE,
    hidden = isHidden
) {
    companion object {
        const val TYPE = 68
    }
}