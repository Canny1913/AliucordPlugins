package com.github.canny1913.adapter.items

class OnboardingItemHeader(
    val currentQuestion: Int,
    val questionCount: Int,
    val question: String,
    val required: Boolean
): OnboardingItem {

    override fun getKey(): String = OnboardingItemType.HEADER.name
    override fun getType(): Int = OnboardingItemType.HEADER.type
}
