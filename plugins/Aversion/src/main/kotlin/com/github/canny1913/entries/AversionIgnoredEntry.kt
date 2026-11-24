package com.github.canny1913.entries

import com.discord.api.user.User

data class AversionIgnoredEntry(
    val ignoredCount: Int,
    val isExpandedIgnoreBlock: Boolean,
    val ignoredAuthors: List<User>,
    val messages: Set<Long>,
    val isHidden: Boolean
): AversionEntry(
    aversionCount = ignoredCount,
    isExpanded = isExpandedIgnoreBlock,
    authors = ignoredAuthors,
    messageIds = messages,
    key = messages.firstOrNull().toString(),
    type = TYPE,
    hidden = isHidden
) {
    companion object {
        const val TYPE = 69
    }
}