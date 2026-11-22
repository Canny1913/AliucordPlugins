package com.github.canny1913.entries

import com.discord.api.user.User
import com.discord.models.message.Message

data class AversionIgnoredEntry(
    val ignoredCount: Int,
    val isExpandedIgnoreBlock: Boolean,
    val ignoredAuthors: List<User>,
    val adapterMessage: Message,
    val isHidden: Boolean
): AversionEntry(
    aversionCount = ignoredCount,
    isExpanded = isExpandedIgnoreBlock,
    authors = ignoredAuthors,
    message = adapterMessage,
    key = adapterMessage.id.toString(),
    type = TYPE,
    hidden = isHidden
) {
    companion object {
        const val TYPE = 69
    }
}