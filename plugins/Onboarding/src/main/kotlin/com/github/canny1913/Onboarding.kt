package com.github.canny1913

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.fragments.FragmentProxy
import com.aliucord.patcher.after
import com.aliucord.patcher.before
import com.aliucord.patcher.component1
import com.aliucord.patcher.component2
import com.aliucord.patcher.component3
import com.aliucord.patcher.component4
import com.aliucord.patcher.instead
import com.aliucord.utils.accessField
import com.aliucord.wrappers.ChannelWrapper.Companion.guildId
import com.aliucord.wrappers.GuildMemberWrapper.Companion.avatar
import com.aliucord.wrappers.GuildMemberWrapper.Companion.isPending
import com.aliucord.wrappers.GuildMemberWrapper.Companion.joinedAt
import com.aliucord.wrappers.GuildMemberWrapper.Companion.nick
import com.aliucord.wrappers.GuildMemberWrapper.Companion.premiumSince
import com.aliucord.wrappers.GuildMemberWrapper.Companion.roles
import com.discord.api.guildjoinrequest.ApplicationStatus
import com.discord.api.message.reaction.MessageReaction
import com.discord.api.presence.Presence
import com.discord.api.user.User
import com.discord.api.utcdatetime.UtcDateTime
import com.discord.app.AppActivity
import com.discord.stores.StoreGuilds
import com.discord.stores.StoreStream
import com.discord.widgets.chat.input.ChatInputViewModel
import com.discord.widgets.chat.input.WidgetChatInput
import com.discord.widgets.chat.input.`WidgetChatInput$configureChatGuard$6`
import com.discord.widgets.chat.input.`WidgetChatInput$configureChatGuard$7`
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterEventsHandler
import com.discord.widgets.servers.member_verification.`MemberVerificationSuccessDialog$configureLoadedUI$1`
import java.lang.reflect.Method
import com.discord.api.guildmember.GuildMember as ApiGuildMember
import com.discord.models.member.GuildMember as ModelGuildMember

@AliucordPlugin(
    requiresRestart = true // or just exit main activity in some way
)
class Onboarding : Plugin() {
    // fixme: find a better solution
    var pluginContext: PluginContext? = null
    val handleGuildMemberAddMethod: Method =
        StoreStream::class.java.getDeclaredMethod("handleGuildMemberAdd", ApiGuildMember::class.java)
            .apply { isAccessible = true }
    var ModelGuildMember.flags by accessField<Int?>()
    var ApiGuildMember.flags by accessField<Int?>()
    var ChatInputViewModel.ViewState.Loaded.shouldShowVerificationGateField by accessField<Boolean?>()
    var ChatInputViewModel.ViewState.Loaded.isInputShowingField by accessField<Boolean?>()
    var ChatInputViewModel.ViewState.Loaded.joinRequestStatusField by accessField<ApplicationStatus?>()

    override fun start(context: Context) {
        patchFragmentProxy() // todo: dont run this for aliucore
        patchInit()
        patchViews()
        patchModels()
        patchMisc()
    }
    fun patchFragmentProxy() {
        patcher.after<FragmentProxy>("onCreateView", LayoutInflater::class.java, ViewGroup::class.java, Bundle::class.java) {
            val fragment = getmFragment()
            val mAdded = Fragment::class.java.getDeclaredField("mAdded").apply { isAccessible = true }
            mAdded[fragment] = mAdded[this]

            val viewLifecycleOwnerField = Fragment::class.java.getDeclaredField("mViewLifecycleOwner").apply { isAccessible = true }
            viewLifecycleOwnerField[fragment] = viewLifecycleOwnerField[this]

            val fragmentProxyViewField = FragmentProxy::class.java.getDeclaredField("mView").apply { isAccessible = true }
            val fragmentViewField = Fragment::class.java.getDeclaredField("mView").apply { isAccessible = true }
            fragmentViewField[fragment] = fragmentProxyViewField[this]
        }
    }

    fun patchInit() {
        patcher.after<AppActivity>("onCreate", Bundle::class.java) {
            pluginContext = PluginContext(this, this@Onboarding)
        }
        // backwards compatibility with official aliu
        try {
            pluginContext = PluginContext(Utils.appActivity, this@Onboarding)
        } catch (e: Exception) {
            logger.error(IllegalStateException("Activity is uninitialized!", e))
        }

    }
    fun patchViews() {
        // forcefully enable chatguard if onboarding is required
        patcher.before<WidgetChatInput>("configureUI",
            ChatInputViewModel.ViewState::class.java
        ) { (_, viewState: ChatInputViewModel.ViewState) ->
            val loaded = viewState as? ChatInputViewModel.ViewState.Loaded ?: return@before
            if (shouldOpenOnboarding(loaded.channel.guildId)) {
                loaded.shouldShowVerificationGateField = true
                loaded.joinRequestStatusField = ApplicationStatus.STARTED
                loaded.isInputShowingField = false
            }
        }
        // check for onboarding requirement in every place where member verification is checked
        patcher.before<`WidgetChatInput$configureChatGuard$6`>("onClick", View::class.java) { param ->
            if (maybeOpenOnboarding(pluginContext!!, this.`$viewState`.channel.guildId)) param.result = null
        }
        patcher.before<`WidgetChatInput$configureChatGuard$7`>("onClick", View::class.java) { param ->
            if (maybeOpenOnboarding(pluginContext!!, this.`$viewState`.channel.guildId)) param.result = null
        }
        patcher.before<WidgetChatListAdapterEventsHandler>(
            "onReactionLongClicked",
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!,
            MessageReaction::class.java
        ) { param ->
            if (maybeOpenOnboarding(pluginContext!!, param.args[0] as Long)) param.result = null
        }
        patcher.before<WidgetChatListAdapterEventsHandler>(
            "onReactionClicked",
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!,
            MessageReaction::class.java,
            Boolean::class.javaPrimitiveType!!
        ) { param ->
            if (param.args[5] as Boolean) {
                if (maybeOpenOnboarding(pluginContext!!, param.args[0] as Long)) param.result = null
            }
        }
    }
    fun patchModels() {
        patcher.after<ApiGuildMember?>(
            "a",
            ApiGuildMember::class.java,
            Long::class.javaPrimitiveType!!,
            User::class.java,
            List::class.java,
            String::class.java,
            String::class.java,
            UtcDateTime::class.java,
            Boolean::class.java,
            Presence::class.java,
            Long::class.javaObjectType,
            String::class.java,
            String::class.java,
            String::class.java,
            UtcDateTime::class.java,
            Int::class.javaPrimitiveType!!,
        ) { (param, old: ApiGuildMember) ->
            val res = param.result as ApiGuildMember
            res.flags = old.flags
        }

        patcher.after<ModelGuildMember.Companion>(
            "from",
            ApiGuildMember::class.java,
            Long::class.javaPrimitiveType!!,
            Map::class.java,
            StoreGuilds::class.java,
        ) { (param, api: ApiGuildMember) ->
            val res = param.result as ModelGuildMember
            res.flags = api.flags
        }
        patcher.after<ModelGuildMember>(
            "copy",
            Int::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!,
            List::class.java,
            String::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType!!,
            UtcDateTime::class.java,
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!,
            String::class.java,
            String::class.java,
            String::class.java,
            UtcDateTime::class.java,
        ) { (param) ->
            val res = param.result as ModelGuildMember
            res.flags = this.flags
        }

        patcher.after<ModelGuildMember>(
            "equals",
            Any::class.java,
        ) { (param, other: Any) ->
            val otherMember = other as? ModelGuildMember ?: return@after
            val result = param.result as Boolean
            param.result = result && (otherMember.flags == flags)
        }
        patcher.after<ApiGuildMember>(
            "equals",
            Any::class.java,
        ) { (param, other: Any) ->
            val otherMember = other as? ApiGuildMember ?: return@after
            val result = param.result as Boolean
            param.result = result && (otherMember.flags == flags)
        }
        patcher.instead<StoreStream>(
            "synthesizeGuildMemberAdd",
            Long::class.javaObjectType,
            User::class.java,
            ApiGuildMember::class.java
        ) { (_, guildId: Long?, user: User?, member: ApiGuildMember?) ->
            // fixme: this is not an ideal solution, replacing store methods bad
            synthesizeGuildMemberAddWithFlags(guildId, user, member)
        }
    }
    fun patchMisc() {
        // Actually dismiss verification success dialog when button is clicked
        patcher.after<`MemberVerificationSuccessDialog$configureLoadedUI$1`>("onClick", View::class.java) {
            this.`this$0`.dismiss()
        }
        // disable reified function marker for viewmodel delegates
        patcher.instead<d0.z.d.m?>(
            "reifiedOperationMarker", Int::class.javaPrimitiveType!!, String::class.java
        ) {}
    }

    fun StoreStream.synthesizeGuildMemberAddWithFlags(guildId: Long?, user: User?, member: ApiGuildMember?) {
        if (guildId == null || user == null || member == null || member.l() == null) { // member.roles
            return
        }
        handleGuildMemberAddMethod(
            this, ApiGuildMember(
                guildId,
                user,
                member.roles,
                member.nick,
                member.premiumSince,
                member.joinedAt,
                member.isPending,
                null,
                null,
                member.avatar,
                null,
                null,
                member.e(), // GuildMember.communicationDisabledUntil
            ).apply { flags = member.flags })
    }

    override fun stop(context: Context) = patcher.unpatchAll()

    fun maybeOpenOnboarding(ctx: Context, guildId: Long): Boolean {
        if ((shouldOpenOnboarding(guildId))) {
            Utils.openPageWithProxy(ctx, OnboardingFragment(ctx, guildId))
            return true
        }
        return false
    }

    fun shouldOpenOnboarding(guildId: Long, member: ModelGuildMember? = null): Boolean {
        val storeStream = StoreStream.Companion!!
        val guild = storeStream.guilds.getGuild(guildId)
        val meId = storeStream.users.me.id
        val guildMember = member ?: storeStream.guilds.getMember(guildId, meId) ?: return false
        val flag = guildMember.flags ?: return false
        val onboardingCompleted = (flag and GuildMemberFlags.COMPLETED_ONBOARDING.value) != 0
        if (onboardingCompleted) {
            return false
        }

        val hasOnboardingFeature =
            guild.features?.any { it?.name == "GUILD_ONBOARDING_HAS_PROMPTS" || it?.name == "GUILD_ONBOARDING" } == true
        return hasOnboardingFeature
    }
}

@Suppress("unused")
enum class GuildMemberFlags(val value: Int) {
    DID_REJOIN(1 shl 0),
    COMPLETED_ONBOARDING(1 shl 1),
    BYPASSES_VERIFICATION(1 shl 2),
    STARTED_ONBOARDING(1 shl 3),
    IS_GUEST(1 shl 4),
    STARTED_HOME_ACTIONS(1 shl 5),
    COMPLETED_HOME_ACTIONS(1 shl 6),
    AUTOMOD_QUARANTINED_NAME(1 shl 7),
    AUTOMOD_QUARANTINED_BIO(1 shl 8),
    DM_SETTINGS_UPSELL_ACKNOWLEDGED(1 shl 9),
    AUTOMOD_QUARANTINED_GUILD_TAG(1 shl 10),
}

