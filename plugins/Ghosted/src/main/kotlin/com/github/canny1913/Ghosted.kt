package com.github.canny1913


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.GatewayAPI
import com.aliucord.entities.Plugin
import com.aliucord.fragments.ConfirmDialog
import com.aliucord.patcher.after
import com.aliucord.patcher.before
import com.aliucord.patcher.component1
import com.aliucord.patcher.component2
import com.aliucord.patcher.component3
import com.aliucord.utils.ChannelUtils
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.GsonUtils
import com.aliucord.utils.SerializedName
import com.aliucord.utils.ViewUtils.addTo
import com.aliucord.utils.accessField
import com.aliucord.utils.accessGetter
import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.aliucord.wrappers.ChannelWrapper.Companion.type
import com.discord.api.message.Message
import com.discord.app.AppActivity
import com.discord.databinding.WidgetChannelsListBinding
import com.discord.databinding.WidgetChannelsListItemActionsBinding
import com.discord.databinding.WidgetChannelsListItemChannelPrivateBinding
import com.discord.models.domain.ModelMessageDelete
import com.discord.stores.StoreStream
import com.discord.widgets.channels.list.WidgetChannelsList
import com.discord.widgets.channels.list.WidgetChannelsListAdapter
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions
import com.discord.widgets.channels.list.items.ChannelListItem
import com.discord.widgets.channels.list.items.ChannelListItemPrivate
import com.github.canny1913.ObservableUtils.loggerSubscribe
import com.github.canny1913.ObservableUtils.withComputation
import com.github.canny1913.settings.GhostedSettings
import com.google.gson.reflect.TypeToken
import java.lang.ref.WeakReference

@AliucordPlugin(
    requiresRestart = true
)
class Ghosted : Plugin() {
    var WidgetChannelsListAdapter.ItemChannelPrivate.binding by accessField<WidgetChannelsListItemChannelPrivateBinding>()
    val WidgetChannelsListItemChannelActions.binding by accessGetter<WidgetChannelsListItemActionsBinding>()
    var me: Long = 0L

    init {
        settingsTab = SettingsTab(
            GhostedSettings::class.java, SettingsTab.Type.PAGE
        )
    }

    override fun start(context: Context) {
        observeApi()
        patchInit()
        patchWidgets()
        patchStores()
    }

    override fun stop(context: Context) {
        pluginCtx = null
        msgCache.clear()
        mutedDmsCache.clear()
        patcher.unpatchAll()
    }

    fun patchInit() {
        patcher.after<AppActivity>("onCreate", Bundle::class.java) {
            pluginCtx = PluginContext(this, this@Ghosted)
        }
        // backwards compatibility with official aliu
        try {
            pluginCtx = PluginContext(Utils.appActivity, this@Ghosted)
        } catch (e: Exception) {
            logger.error(IllegalStateException("Activity is uninitialized!", e))
        }
    }

    fun observeApi() {
        StoreStream.getUsers().observeMeId().withComputation().loggerSubscribe { me = it }
        StoreStream.Companion!!.userGuildSettings.observeGuildSettings(0).withComputation()
            .loggerSubscribe { settings ->
                settings.channelOverrides.map { it.channelId }.toHashSet()
            }
        GatewayAPI.onEvent<Any>("READY") { refreshAll() }
        GatewayAPI.onEvent<Any>("RESUMED") { refreshAll() }
    }

    fun patchWidgets() {
        patcher.after<WidgetChannelsListAdapter.ItemChannelPrivate>(
            "onConfigure",
            Int::class.java,
            ChannelListItem::class.java,
        ) { (_, _: Int, item: ChannelListItemPrivate) ->
            msgCache[item.channel.id]?.let { message ->
                val textView = binding.f
                val isGroup = item.channel.type == 3
                val isCurrentUser = message.author == me
                val clearedStamp = GhostedSettings.clearMarkerTimestamps[item.channel.id] ?: 0
                val skipCheck =
                    ((message.bot == true && GhostedSettings.ignoreBots)
                            || (isGroup && GhostedSettings.ignoreGroupDms)
                            || (item.channel.id in mutedDmsCache && GhostedSettings.ignoreMutedDms)
                            || message.timestamp <= clearedStamp
                            || isCurrentUser)

                val isInactive =
                    (System.currentTimeMillis() - message.timestamp) > GhostedSettings.ghostedTime.toMs() && !skipCheck
                if (isInactive) {
                    textView.compoundDrawablePadding = 8.dp
                    textView.setCompoundDrawables(null, null, ghostIcon, null)
                } else {
                    textView.compoundDrawablePadding = 0
                    textView.setCompoundDrawables(null, null, null, null)
                }
            }
        }

        patcher.after<WidgetChannelsListItemChannelActions>(
            "configureUI", WidgetChannelsListItemChannelActions.Model::class.java
        ) { (_, model: WidgetChannelsListItemChannelActions.Model) ->
            if (ChannelUtils.getRecipients(model.channel).isNullOrEmpty()) return@after
            val layout = binding.h.parent as LinearLayout
            val index = layout.indexOfChild(binding.h)
            val textView = LayoutInflater.from(pluginCtx!!)
                .inflate(R.layout.channel_action_ghost_clear, layout, false) as TextView
            textView.apply {
                setOnClickListener {
                    onClickClear(this@after, model.channel.id)
                }
                addTo(layout, index)
            }
        }
        patcher.after<WidgetChannelsListAdapter>(
            RecyclerView::class.java,
            FragmentManager::class.java,
        ) {
            adapterRef = WeakReference(this)
        }
    }

    fun patchStores() {
        patcher.before<StoreStream>(
            "handleMessageCreate", Message::class.java
        ) { (_, msg: Message) ->
            handleMessageUpdate(msg)
        }

        patcher.before<StoreStream>(
            "handleMessageUpdate", Message::class.java
        ) { (_, msg: Message) ->
            handleMessageUpdate(msg)
        }

        patcher.before<StoreStream>(
            "handleMessageDelete", ModelMessageDelete::class.java
        ) { (_, deleteModel: ModelMessageDelete) ->
            handleMessageDelete(deleteModel)
        }
    }

    fun onClickClear(fragment: Fragment, id: Long) {
        val dialog = ConfirmDialog().setTitle("Clear Ghost")
            .setDescription("Are you sure you want to clear ghost for this channel?")

        dialog.setOnOkListener {
            GhostedSettings.clearMarkerTimestamps = GhostedSettings.clearMarkerTimestamps.apply {
                this[id] = msgCache[id]?.timestamp ?: 0
            }
            Utils.showToast("Cleared ghost")
            rerender(id)
            dialog.dismiss()
        }.show(fragment.parentFragmentManager, "Clear Ghost")
    }

    private fun handleMessageUpdate(msg: Message) {
        val gid = msg.m()
        if (gid == null) {
            val channelId = msg.g()

            val oldMsgId = msgCache[channelId]?.id ?: 0
            if (msg.o() > oldMsgId) {
                msgCache[channelId] = msg.toItem()
                rerender(channelId)
            }
        }
    }

    private fun handleMessageDelete(deleteModel: ModelMessageDelete) {
        msgCache[deleteModel.channelId]?.let { msg ->
            if (msg.id in deleteModel.messageIds) {
                msgCache.remove(deleteModel.channelId)
                rerender(deleteModel.channelId)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun refreshAll() {
        val channels = StoreStream.getChannels().getChannelsForGuild(0)
            .filterValues { it.D() == 1 || it.D() == 3 } // type == Type.DM && type == Type.GROUP_DM
            .keys.take(100)
        Utils.threadPool.execute {
            val res = Http.Request.newDiscordRNRequest("/channels/preload-messages", "POST")
                .executeWithJson(ChannelIdsPayload(channels))
                .json<List<Message>>(GsonUtils.gsonRestApi, responseType)
            msgCache = mutableMapOf(*res.map { it.g() to it.toItem() }.toTypedArray())

            Utils.mainThread.post {
                @SuppressLint("NotifyDataSetChanged") adapterRef.get()?.notifyDataSetChanged()
            }
        }
    }

    private fun rerender(id: Long) {
        val adapter = adapterRef.get() ?: return
        val idx = adapter.internalData.indexOfFirst { it.key == "3$id" }
        if (idx != -1) {
            Utils.mainThread.post {
                adapter.notifyItemChanged(idx, Any())
            }
        }
    }

    private val responseType =
        TypeToken.getParameterized(List::class.java, Message::class.java).type

    data class ChannelIdsPayload(
        @SerializedName("channel_ids") val channelIds: List<Long>,
    )

    data class MessageItem(
        val id: Long, val timestamp: Long, val author: Long, val bot: Boolean?
    )

    fun Message.toItem() = MessageItem(
        id = this.o(), author = this.e().id, timestamp = this.D().g(), bot = this.e().e()
    )

    companion object {
        var msgCache = mutableMapOf<Long, MessageItem>()
        var mutedDmsCache = mutableSetOf<Long>()
        var adapterRef: WeakReference<WidgetChannelsListAdapter> = WeakReference(null)

        @SuppressLint("StaticFieldLeak")
        internal var pluginCtx: PluginContext? = null
        val ghostIcon: Drawable?
            get() = ContextCompat.getDrawable(
                pluginCtx!!, R.drawable.ghost
            )?.apply { setBounds(0, 0, 16.dp, 16.dp) }
    }
}