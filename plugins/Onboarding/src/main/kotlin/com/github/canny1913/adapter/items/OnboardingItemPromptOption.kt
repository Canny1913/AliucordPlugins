package com.github.canny1913.adapter.items

import com.discord.models.domain.emoji.ModelEmojiCustom

class OnboardingItemPromptOption(
    val id: Long,
    val promptIndex: Int,
    val emoji: ModelEmojiCustom,
    val title: String,
    val roleIds: Array<Long>,
    val description: String?,
    val selected: Boolean,
): OnboardingItem {

    override fun getKey(): String = OnboardingItemType.PROMPT_MULTIPLE_CHOICE.name
    override fun getType(): Int = OnboardingItemType.PROMPT_MULTIPLE_CHOICE.type
}
