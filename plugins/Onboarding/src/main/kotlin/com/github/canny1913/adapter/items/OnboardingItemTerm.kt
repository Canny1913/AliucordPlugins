package com.github.canny1913.adapter.items

import com.discord.api.role.GuildRole
import com.discord.simpleast.core.node.Node
import com.discord.utilities.textprocessing.MessageRenderContext



class OnboardingItemTerm(
    val index: Int,
    val ast: MutableList<Node<MessageRenderContext>>,
    val channelNames: Map<Long, String>,
    val roles: Map<Long, GuildRole>,
    val isFirstItem: Boolean,
    val isLastItem: Boolean,
    val allowAnimatedEmojis: Boolean,
): OnboardingItem {
    override fun getKey(): String = OnboardingItemType.VERIFICATION_TERM.name
    override fun getType(): Int = OnboardingItemType.VERIFICATION_TERM.type
}
