package com.github.canny1913.models

import com.aliucord.utils.SerializedName

data class PartialReadyEvent(
    @SerializedName("relationships")
    val relationships: List<UserRelationshipEvent>
)
