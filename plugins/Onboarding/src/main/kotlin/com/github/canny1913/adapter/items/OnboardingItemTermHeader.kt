package com.github.canny1913.adapter.items

class OnboardingItemTermHeader : OnboardingItem {
    override fun getKey(): String = OnboardingItemType.VERIFICATION_TERM_HEADER.name
    override fun getType(): Int = OnboardingItemType.VERIFICATION_TERM_HEADER.type
}
