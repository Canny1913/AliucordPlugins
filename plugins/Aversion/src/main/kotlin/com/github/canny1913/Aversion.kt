package com.github.canny1913

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import b.a.a.d.a
import b.a.a.d.`a$f`
import b.a.a.d.f
import com.aliucord.Http
import com.aliucord.Main
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.GatewayAPI
import com.aliucord.api.PatcherAPI
import com.aliucord.api.Unpatch
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.patcher.before
import com.aliucord.patcher.component1
import com.aliucord.patcher.component2
import com.aliucord.patcher.component3
import com.aliucord.patcher.component4
import com.aliucord.patcher.component5
import com.aliucord.utils.ChannelUtils
import com.aliucord.utils.RxUtils.map
import com.aliucord.utils.ViewUtils.addTo
import com.aliucord.utils.ViewUtils.findViewById
import com.aliucord.utils.accessField
import com.aliucord.wrappers.ChannelWrapper.Companion.recipients
import com.discord.api.channel.Channel
import com.discord.app.AppActivity
import com.discord.databinding.WidgetChannelsListItemChannelPrivateBinding
import com.discord.databinding.WidgetFriendsListAdapterItemFriendBinding
import com.discord.models.message.Message
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.embed.InviteEmbedModel
import com.discord.widgets.channels.list.WidgetChannelsListAdapter
import com.discord.widgets.channels.list.items.ChannelListItem
import com.discord.widgets.channels.list.items.ChannelListItemPrivate
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.adapter.WidgetChatListItem
import com.discord.widgets.chat.list.entries.AttachmentEntry
import com.discord.widgets.chat.list.entries.BlockedMessagesEntry
import com.discord.widgets.chat.list.entries.BotUiComponentEntry
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.entries.EmbedEntry
import com.discord.widgets.chat.list.entries.GameInviteEntry
import com.discord.widgets.chat.list.entries.GiftEntry
import com.discord.widgets.chat.list.entries.GuildTemplateEntry
import com.discord.widgets.chat.list.entries.InviteEntry
import com.discord.widgets.chat.list.entries.MessageEntry
import com.discord.widgets.chat.list.entries.ReactionsEntry
import com.discord.widgets.chat.list.entries.SpotifyListenTogetherEntry
import com.discord.widgets.chat.list.entries.StickerEntry
import com.discord.widgets.chat.list.entries.ThreadEmbedEntry
import com.discord.widgets.chat.list.entries.ThreadStarterDividerEntry
import com.discord.widgets.chat.list.entries.UploadProgressEntry
import com.discord.widgets.chat.list.model.WidgetChatListModel
import com.discord.widgets.chat.list.model.WidgetChatListModelMessages
import com.discord.widgets.chat.list.model.`WidgetChatListModelMessages$Companion$get$1`
import com.discord.widgets.friends.FriendsListViewModel
import com.discord.widgets.friends.WidgetFriendsListAdapter
import com.discord.widgets.home.WidgetHome
import com.discord.widgets.settings.account.WidgetSettingsAccount
import com.github.canny1913.ObservableUtils.loggerSubscribe
import com.github.canny1913.ObservableUtils.ui
import com.github.canny1913.ObservableUtils.withComputation
import com.github.canny1913.entries.AversionBlockedEntry
import com.github.canny1913.entries.AversionIgnoredEntry
import com.github.canny1913.models.PartialReadyEvent
import com.github.canny1913.models.UserRelationshipEvent
import com.github.canny1913.settings.AversionSettings
import com.github.canny1913.stores.StoreIgnoredUsers
import com.github.canny1913.widgets.AversionSettingsIgnoredUsers
import com.github.canny1913.widgets.WidgetAversionMessage
import com.github.canny1913.widgets.WidgetChatIndicator
import de.robv.android.xposed.XC_MethodHook
import org.json.JSONObject
import rx.Observable
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Collections
import java.util.TimeZone


@AliucordPlugin
class Aversion : Plugin() {
    val configureReplySystemMessageMethod: Method =
        WidgetChatListAdapterItemMessage::class.java.getDeclaredMethod(
            "configureReplySystemMessage", Int::class.java
        ).apply { isAccessible = true }
    var backgroundBlockedExpandedField: Field =
        WidgetChatListItem::class.java.getDeclaredField("backgroundBlockedExpanded")
            .apply { isAccessible = true }
    var WidgetChatListItem.backgroundBlockedExpanded: Drawable
        get() = backgroundBlockedExpandedField[this] as Drawable
        set(value) = backgroundBlockedExpandedField.set(this, value)
    var WidgetChannelsListAdapter.ItemChannelPrivate.binding by accessField<WidgetChannelsListItemChannelPrivateBinding>()
    var WidgetFriendsListAdapter.ItemUser.binding by accessField<WidgetFriendsListAdapterItemFriendBinding>()
    var channelListAdapterRef = WeakReference<WidgetChannelsListAdapter>(null)
    var friendListAdapterRef = WeakReference<WidgetFriendsListAdapter>(null)

    val activeColorId = Utils.getResId("colorInteractiveActive", "attr")
    val mutedColorId = Utils.getResId("colorInteractiveMuted", "attr")

    init {
        settingsTab = SettingsTab(
            AversionSettings::class.java, SettingsTab.Type.PAGE
        )
    }

    override fun start(context: Context) {
        patchInit()
        patchChatModels()
        patchChannelList()
        patchStores()
        patchFriendsList()
        patchUserDialog()
        patchSettings()
    }

    fun patchInit() {
        patcher.before<WidgetHome>("onCreate", Bundle::class.java) {
            // we need a custom factory else app won't survive activity recreations
            // fixme: THIS IS A HORRIBLE SOLUTION
            childFragmentManager.fragmentFactory = PluginFragmentFactory()
        }
        patcher.after<AppActivity>("onCreate", Bundle::class.java) {
            pluginCtx = PluginContext(this, this@Aversion)
        }
        // backwards compatibility with official aliu
        try {
            pluginCtx = PluginContext(Utils.appActivity, this@Aversion)
        } catch (e: Exception) {
            logger.error(IllegalStateException("Activity is uninitialized!", e))
        }
    }

    fun patchChatModels() {
        patcher.after<`WidgetChatListModelMessages$Companion$get$1`>(
            "invoke",
            WidgetChatListModelMessages.MessagesWithMetadata::class.java,
            Channel::class.java,
            Map::class.java,
            List::class.java,
            Map::class.java,
            Long::class.javaObjectType,
            Map::class.java,
            Long::class.javaObjectType,
            Boolean::class.java,
            Boolean::class.java,
            Boolean::class.java,
            Long::class.java,
            Map::class.java,
            InviteEmbedModel::class.java,
            Boolean::class.java
        ) { (
                param,
                messagesWithMetadata: WidgetChatListModelMessages.MessagesWithMetadata,
                _: Channel,
                blockedUsersMap: Map<Long, Int>,
                blockedExpanded: List<Long>,
            ) ->
            val result = param.result as? WidgetChatListModelMessages ?: return@after
                processChatEntries(
                    messagesWithMetadata.messages, result.items, blockedUsersMap, blockedExpanded
                )
        }
        patcher.before<WidgetChatListAdapter>(
            "onCreateViewHolder", ViewGroup::class.java, Int::class.java
        ) { (param, _: ViewGroup, viewType: Int) ->
            if (viewType == AversionBlockedEntry.TYPE || viewType == AversionIgnoredEntry.TYPE) {
                param.result = WidgetAversionMessage(this)
            }
        }
        // else chat will not refresh on relationship updates
        patcher.after<WidgetChatListModelMessages.MessagesWithMetadata.Companion>("get",
            Channel::class.java
        ) { param ->
            val result = param.result as? Observable<*> ?: return@after
            param.result = ObservableUtils.combineLatest(result, StoreIgnoredUsers.observe()) { model, _ -> model }.withComputation()
        }

        patcher.after<WidgetChatListItem>(
            "onConfigure", Int::class.java, ChatListEntry::class.java
        ) { param ->
            val data = param.args[1] as? ChatListEntry ?: return@after
            if (data is UploadProgressEntry || adapter.data !is WidgetChatListModel) return@after
            if (data.authorId in StoreIgnoredUsers.ignoredIds) itemView.background = backgroundBlockedExpanded
        }
        patcher.after<WidgetChatListItem>(
            Int::class.java, WidgetChatListAdapter::class.java
        ) {
            backgroundBlockedExpanded = ColorDrawable(blockedColor).mutate()
        }
        patcher.afterBeforeHooks<WidgetChatListAdapterItemMessage>(
            "configureReplyPreview", MessageEntry::class.java
        ) { (param, p0: MessageEntry) ->
            if (p0.replyData?.messageEntry?.message?.author?.id in StoreIgnoredUsers.ignoredIds) {
                // to remove icon etc. without too much reflection spam
                configureReplySystemMessageMethod(this, android.R.string.untitled)
                val spannable =
                    SpannableString(pluginCtx!!.getString(R.string.ignored_reply_message))
                spannable.setSpan(StyleSpan(2), 0, spannable.length, 33)
                WidgetChatListAdapterItemMessage.`configureReplyText$default`(
                    this, spannable, 0.64f, 0, null
                )
                param.result = null
            }
        }
        patcher.before<WidgetHome>(
            "onViewBound", View::class.java
        ) {
            val rootView = requireView() as ViewGroup
            if (rootView.findViewById<View>(R.id.indicator_fragment_container) != null) return@before
            val chatStatusView = rootView.findViewById<FragmentContainerView>("widget_status")
            val chatStatusParent = chatStatusView.parent as ViewGroup
            val chatStatusViewIndex = chatStatusParent.indexOfChild(chatStatusView)
            val fragmentContainer = FragmentContainerView(pluginCtx!!).apply {
                id = R.id.indicator_fragment_container
                visibility = View.VISIBLE
                layoutParams = ConstraintLayout.LayoutParams(0, WRAP_CONTENT).apply {
                    verticalBias = 0f
                    topToBottom = Utils.getResId("widget_status", "id")
                    bottomToBottom = PARENT_ID
                    endToEnd = PARENT_ID
                    startToStart = PARENT_ID
                }
            }
            val fragment = WidgetChatIndicator()
            fragmentContainer.addTo(chatStatusParent, chatStatusViewIndex)
            val transaction = childFragmentManager.beginTransaction()
            transaction.replace(R.id.indicator_fragment_container, fragment).commit()
        }
    }

    fun patchChannelList() {
        StoreIgnoredUsers.observeUpdates().ui().loggerSubscribe { event ->
            channelListAdapterRef.get()?.run {
                if (selectedGuildId != 0L) return@loggerSubscribe
                val idx = internalData.indexOfFirst { item ->
                    item is ChannelListItemPrivate && item.channel.recipients?.singleOrNull()?.id == event.id
                }
                // we need to provide payload else view layout gets postponed
                recycler.post { notifyItemChanged(idx, Any()) }
            }
            friendListAdapterRef.get()?.run {
                val idx = internalData.indexOfFirst { item ->
                    item is FriendsListViewModel.Item.Friend && item.user.id == event.id
                }
                // we need to provide payload else view layout gets postponed
                recycler.post { notifyItemChanged(idx, Any()) }
            }
        }
        // register observable for listening to relationship changes
        patcher.after<WidgetChannelsListAdapter>(
            RecyclerView::class.java,
            FragmentManager::class.java,
        ) {
            channelListAdapterRef = WeakReference(this)
        }
        patcher.after<WidgetFriendsListAdapter>(
            RecyclerView::class.java
        ) {
            friendListAdapterRef = WeakReference(this)
        }
        // Add indicators to DM list
        patcher.after<WidgetChannelsListAdapter.ItemChannelPrivate>(
            "onConfigure", Int::class.java, ChannelListItem::class.java
        ) { (_, _: Int, item: ChannelListItemPrivate) ->
            val userId = ChannelUtils.getDMRecipient(item.channel)?.id ?: return@after
            val isBlocked = userId in blockedUsers
            val isIgnored = userId in StoreIgnoredUsers.ignoredIds
            val itemView = binding.root as RelativeLayout
            var indicatorView = itemView.findViewById<ImageView>(R.id.aversion_channel_indicator)
            if (indicatorView == null) {
                LayoutInflater.from(pluginCtx!!).inflate(R.layout.aversion_channel_list_indicator, itemView)
                indicatorView = itemView.findViewById(R.id.aversion_channel_indicator)
            }

            if (!isIgnored && !isBlocked) {
                indicatorView.visibility = View.GONE
                return@after
            }
            indicatorView.visibility = View.VISIBLE

            // prioritize blocked in case target is both ignored and blocked
            val image = if (isBlocked) blockedDrawable else ignoredDrawable
            indicatorView.setImageDrawable(image)

            val color = if (item.selected) {
                ColorCompat.getThemedColor(binding.f, activeColorId)
            } else {
                ColorCompat.getThemedColor(binding.f, mutedColorId)
            }
            binding.f.setTextColor(color)
            indicatorView.imageTintList = ColorStateList.valueOf(color)
        }
    }
    fun patchStores() {
        GatewayAPI.onEvent<PartialReadyEvent>("READY") {
            dispatch {
                StoreIgnoredUsers.handleConnectionOpen(
                    it
                )
            }
        }
        GatewayAPI.onEvent<UserRelationshipEvent>("RELATIONSHIP_ADD") {
            dispatch {
                StoreIgnoredUsers.handleRelationshipUpdate(
                    it
                )
            }
        }
        GatewayAPI.onEvent<UserRelationshipEvent>("RELATIONSHIP_UPDATE") { event ->
            dispatch {
                StoreIgnoredUsers.handleRelationshipUpdate(
                    event
                )
            }
        }
        GatewayAPI.onEvent<UserRelationshipEvent>("RELATIONSHIP_REMOVE") {
            dispatch {
                StoreIgnoredUsers.handleRelationshipUpdate(
                    it
                )
            }
        }
        val storeStream = StoreStream.Companion!!
        storeStream.authentication.`preLogoutSignal$app_productionGoogleRelease`.loggerSubscribe {
            StoreIgnoredUsers.handlePreLogout()
        }

        storeStream.userRelationships.observeForType(2).withComputation().map { it.keys }
            .loggerSubscribe {
                blockedUsers.clear()
                blockedUsers.addAll(it)
            }
    }

    fun patchFriendsList() {
        patcher.before<FriendsListViewModel>(
            "getItems",
            Map::class.java,
            Map::class.java,
            Map::class.java,
            Map::class.java,
            Boolean::class.java,
            Map::class.java
        ) { (
                param,
                relationships: Map<Long, Int>,
            ) ->
            param.args[0] = relationships.filterValues { it != 0 }
        }
        patcher.after<WidgetFriendsListAdapter.ItemUser>(
            "onConfigure", Int::class.java, FriendsListViewModel.Item::class.java
        ) { (_, _: Int, p1: FriendsListViewModel.Item.Friend) ->
            if (p1.user.id in StoreIgnoredUsers.ignoredIds) {
                binding.b.text = pluginCtx!!.getString(R.string.friends_list_ignored)
            }
        }
    }

    fun patchUserDialog() {
        patcher.after<`a$f`>("invoke",
            Any::class.java
        ) { (_, viewModel: f.d.a) ->
            val dialog = receiver as a
            val binding = dialog.g()
            val user = viewModel.a
            val isIgnored = user.id in StoreIgnoredUsers.ignoredIds
            val textView = LayoutInflater.from(pluginCtx!!).inflate(R.layout.user_actions_ignore, binding.a, false) as TextView
            textView.addTo(binding.a)
            val textId = if (isIgnored) R.string.user_actions_unignore else R.string.user_actions_ignore
            textView.text = pluginCtx!!.getString(textId)
            textView.setOnClickListener {
                if (isIgnored) {
                    sendIgnoreRequest(user.id, "DELETE", USER_ACTIONS_UNIGNORE_LOCATION) {
                        val err = it ?: return@sendIgnoreRequest dialog.dismiss()
                        logger.error("Failed to unignore", err)
                        logger.errorToast(textView.context.getString(R.string.failed_to_unignore), err)
                    }
                } else {
                    sendIgnoreRequest(user.id, "PUT", USER_ACTIONS_IGNORE_LOCATION) {
                        val err = it ?: return@sendIgnoreRequest dialog.dismiss()
                        logger.error("Failed to ignore", err)
                        logger.errorToast(textView.context.getString(R.string.failed_to_ignore), err)
                    }
                }
            }
        }
    }
    fun patchSettings() {
        patcher.after<WidgetSettingsAccount>("configureUI",
            WidgetSettingsAccount.Model::class.java
        ) {
            val binding = WidgetSettingsAccount.`access$getBinding$p`(this)
            var ignoredUsersView = binding.root.findViewById<TextView>(R.id.settings_ignored_users_button)
            if (ignoredUsersView == null) {
                val blockedUsersView = binding.G
                val parent = blockedUsersView.parent as LinearLayout
                val idx = parent.indexOfChild(blockedUsersView)
                ignoredUsersView = LayoutInflater.from(pluginCtx!!).inflate(R.layout.ignored_users_button, parent, false).addTo(parent, idx) as TextView
            }
            ignoredUsersView.setOnClickListener {
                Utils.openPageWithProxy(it.context, AversionSettingsIgnoredUsers())
            }
        }
    }
    fun processChatEntries(
        messages: List<Message>,
        items: MutableList<ChatListEntry>,
        blockedUsersMap: Map<Long, Int>,
        blockedExpanded: List<Long>
    ) {
        val hideBlockedEntries = AversionSettings.hideBlockedMessages
        val hideIgnoredEntries = AversionSettings.hideIgnoredMessages
        val blockedGroups = mutableListOf(mutableListOf<Message>())
        val ignoredUsers = StoreIgnoredUsers.ignoredIds
        val ignoreGroups = mutableListOf<Pair<Int, Int>>()
        var ignoreGroupStart = -1
        messages.forEachIndexed { index, currentMsg ->
            if (currentMsg.author.id in blockedUsersMap) {
                val lastGroup = blockedGroups.last()
                val lastGroupMessage = lastGroup.lastOrNull()
                val lastGroupMessageTimestamp = lastGroupMessage?.timestamp?.g()
                if (currentMsg.timestamp.g() equalsDay lastGroupMessageTimestamp) {
                    lastGroup.add(currentMsg)
                } else {
                    blockedGroups.add(mutableListOf(currentMsg))
                }
            } else if (blockedGroups.lastOrNull()
                    ?.isNotEmpty() == true
            ) blockedGroups += mutableListOf<Message>()

            if (currentMsg.author.id in ignoredUsers) {
                val nextGroupMessage =
                    messages.getOrNull(index + 1).takeIf { it?.author?.id in ignoredUsers }
                val nextGroupMessageTimestamp = nextGroupMessage?.timestamp?.g()
                if (currentMsg.timestamp.g() equalsDay nextGroupMessageTimestamp) {
                    if (ignoreGroupStart == -1) ignoreGroupStart = index
                    return@forEachIndexed
                } else if (ignoreGroupStart != -1) {
                    ignoreGroups += ignoreGroupStart to index + 1
                    ignoreGroupStart = -1
                } else {
                    ignoreGroups += index to index + 1
                }
            }
        }
        ignoreGroups.forEach { (start, end) ->
            val msgSubList = messages.subList(start, end)
            val ignoredAuthors = msgSubList.map { it.author }.distinctBy { it.id }
            val firstItem = items.indexOfFirst { it.messageId == msgSubList.last().id }
            val lastItem = items.indexOfLast { it.messageId == msgSubList.first().id }
            val itemSubList = items.subList(firstItem, lastItem + 1)
            val ignoredSize = msgSubList.size
            val isExpanded = msgSubList.first().id in blockedExpanded
            if (!isExpanded) {
                itemSubList.clear()
            }
            itemSubList.add(
                AversionIgnoredEntry(
                    ignoredCount = ignoredSize,
                    isExpandedIgnoreBlock = isExpanded,
                    ignoredAuthors = ignoredAuthors,
                    adapterMessage = msgSubList.first(),
                    isHidden = hideIgnoredEntries
                )
            )
        }
        val processedBlockedGroups = blockedGroups.filter { it.isNotEmpty() }.reversed()
        val originalBlockedEntries = items.withIndex().filter { it.value is BlockedMessagesEntry }

        originalBlockedEntries.forEachIndexed { groupIndex, (entryIndex, _) ->
            val originalEntry = items[entryIndex] as BlockedMessagesEntry
            val groupMsgs = processedBlockedGroups[groupIndex]
            val blockedAuthors = groupMsgs.map { it.author }.distinctBy { it.id }
            val newEntry = AversionBlockedEntry(
                blockedCount = groupMsgs.size,
                isExpandedBlock = originalEntry.isInExpandedBlockedMessageChunk,
                blockedAuthors = blockedAuthors,
                adapterMessage = originalEntry.message,
                isHidden = hideBlockedEntries
            )
            if (originalEntry.isInExpandedBlockedMessageChunk) {
                val lastMessageId = groupMsgs.minOf { it.id }
                var newIndex = items.indexOfLast {
                    it.messageId == lastMessageId
                }
                items.removeAt(entryIndex)
                if (messages.size == 1) newIndex += 1
                items.add(newIndex, newEntry)
            } else {
                items[entryIndex] = newEntry
            }
        }


    }

    val ChatListEntry.messageId: Long?
        get() {
            return when (this) {
                is MessageEntry -> this.message.id
                is AttachmentEntry -> this.message.id
                is EmbedEntry -> this.message.id
                is StickerEntry -> this.message.id
                is GameInviteEntry -> this.messageId
                is SpotifyListenTogetherEntry -> this.messageId
                is InviteEntry -> this.messageId
                is GuildTemplateEntry -> this.messageId
                is GiftEntry -> this.messageId
                is ThreadStarterDividerEntry -> this.messageId
                is BotUiComponentEntry -> this.message.id
                is ReactionsEntry -> this.message.id
                is ThreadEmbedEntry -> this.messageId
                else -> null
            }
        }

    val ChatListEntry.authorId: Long?
        get() {
            return when (this) {
                is MessageEntry -> this.message.author.id
                is AttachmentEntry -> this.message.author.id
                is EmbedEntry -> this.message.author.id
                is StickerEntry -> this.message.author.id
                is BotUiComponentEntry -> this.message.author.id
                is ReactionsEntry -> this.message.author.id
                is GameInviteEntry -> this.authorId
                is SpotifyListenTogetherEntry -> this.userId
                is InviteEntry -> this.userId
                is GuildTemplateEntry -> this.userId
                is GiftEntry -> this.userId
                is ThreadEmbedEntry -> this.parentMessage.author.id
                else -> null
            }
        }

    override fun stop(context: Context) {
        pluginCtx = null
        patcher.unpatchAll()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        internal var pluginCtx: PluginContext? = null

        internal var blockedUsers: MutableSet<Long> = Collections.synchronizedSet(hashSetOf<Long>())
        val blockedColor = ColorCompat.getColor(
            Utils.appContext, Utils.getResId("primary_dark_630", "color")
        )

        val ignoredDrawable: Drawable?
            get() = ContextCompat.getDrawable(
                pluginCtx!!, R.drawable.ic_ignored
            )
        val blockedDrawable: Drawable?
            get() = ContextCompat.getDrawable(
                pluginCtx!!, R.drawable.ic_blocked
            )

        fun dispatch(func: () -> Unit) {
            StoreStream.getDispatcherYesThisIsIntentional().schedule(func)
        }

        fun createLocationJson(location: String): String =
            JSONObject().apply { put("location", location) }.toString().toByteArray(Charsets.UTF_8).let { Base64.encodeToString(it, Base64.NO_WRAP) }
        const val USER_ACTIONS_IGNORE_LOCATION: String = "ignore confirmation action sheet"
        const val USER_ACTIONS_UNIGNORE_LOCATION: String = "user profile overflow menu"
        const val SETTINGS_UNIGNORE_LOCATION: String = "ignored-users-list-mobile"

        fun sendIgnoreRequest(userId: Long, method: String, location: String, callback: (Throwable?) -> Unit) {
            Utils.threadPool.execute {
                val req = Http.Request.newDiscordRNRequest(
                    "/users/@me/relationships/${userId}/ignore",
                    method
                ).setHeader("X-Context-Properties", createLocationJson(location)).setRequestTimeout(10000)
                val res = req.execute()
                Utils.mainThread.post {
                    val ex = if (!res.ok()) Http.HttpException(req, res) else null
                    callback(ex)
                }
            }
        }
    }
}

class PluginFragmentFactory : FragmentFactory() {
    override fun instantiate(p0: ClassLoader, p1: String): Fragment {
        return super.instantiate(Aversion::class.java.classLoader!!, p1)
    }
}
val timeDifference = TimeZone.getDefault().rawOffset

val Long.toLocal: Long
    get() = this + timeDifference

infix fun Long.equalsDay(other: Long?): Boolean {
    if (other == null) return false
    return (this.toLocal / 86400000L) == (other.toLocal / 86400000L)
}

inline fun <reified T> PatcherAPI.afterBeforeHooks(
    methodName: String,
    vararg paramTypes: Class<*>,
    crossinline callback: T.(XC_MethodHook.MethodHookParam) -> Unit
): Unpatch {
    return patch(
        T::class.java.getDeclaredMethod(methodName, *paramTypes),
        object : XC_MethodHook(PRIORITY_DEFAULT - 1) {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    callback(param.thisObject as T, param)
                } catch (th: Throwable) {
                    Main.logger.error(
                        "Exception while pre-hooking ${param.method.declaringClass.name}.${param.method.name}",
                        th
                    )
                }
            }
        })
}
