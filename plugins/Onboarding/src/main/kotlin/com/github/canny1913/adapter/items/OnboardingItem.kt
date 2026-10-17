package com.github.canny1913.adapter.items

import com.discord.utilities.mg_recycler.MGRecyclerDataPayload

interface OnboardingItem: MGRecyclerDataPayload

enum class OnboardingItemType(val type: Int) {
    HEADER(0),
    // PROMPT_DROPDOWN(1), // todo: not necessary but do
    PROMPT_MULTIPLE_CHOICE(2),

    // Other verification item types should not occur in onboarding.
    VERIFICATION_TERM_HEADER(3),
    VERIFICATION_TERM(4);
}
