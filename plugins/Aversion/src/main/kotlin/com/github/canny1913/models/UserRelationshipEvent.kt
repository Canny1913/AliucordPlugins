package com.github.canny1913.models

import com.aliucord.utils.SerializedName
import com.discord.api.user.User

data class UserRelationshipEvent(
    @SerializedName("user_id")
    val userId: Long?,
    @SerializedName("id")
    val id: Long,
    @SerializedName("user_ignored")
    val userIgnored: Boolean,
    @SerializedName("user")
    val user: User,
    @SerializedName("type")
    val type: Int,
)
