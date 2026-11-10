package com.github.canny1913

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aliucord.PluginManager
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.fragments.FragmentProxy
import com.aliucord.patcher.after
import com.aliucord.patcher.component1
import com.aliucord.patcher.component2
import com.aliucord.patcher.instead
import com.aliucord.utils.ViewUtils.findViewById
import com.discord.app.AppActivity
import com.discord.models.message.Message
import com.discord.widgets.chat.list.entries.MessageEntry
import com.discord.widgets.search.results.WidgetSearchResults

@AliucordPlugin(
    requiresRestart = true // or just exit main activity in some way
)
class SearchPreview : Plugin() {
    // fixme: find a better solution
    var pluginContext: PluginContext? = null
    override fun start(context: Context) {
        patchFragmentProxy() // todo: dont run this for aliucore
        patchInit()
        patchViews()
        patchMisc()
    }
    fun patchFragmentProxy() {
        if (PluginManager.plugins["Onboarding"]?.manifest?.authors?.any { it?.name == "canny1913" } == true) return

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
            pluginContext = PluginContext(this, this@SearchPreview)
        }
        // backwards compatibility with official aliu
        try {
            pluginContext = PluginContext(Utils.appActivity, this@SearchPreview)
        } catch (e: Exception) {
            logger.error(IllegalStateException("Activity is uninitialized!", e))
        }
    }
    fun patchViews() {
        patcher.instead<WidgetSearchResults.SearchResultAdapterEventHandler>("jumpToChat",
            Message::class.java
        ) { (_, msg: Message) ->
            Utils.openPageWithProxy(pluginContext!!, SearchPreviewFragment(pluginContext!!, msg.channelId, msg.id))
        }
        patcher.after<com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage>("configureReplyPreview",
            MessageEntry::class.java
        ) {
            // todo: bad fix
            // without this, pressing replies on previews will affect chat as well
            if (adapter.data.guildId == 0L) {
                val reply = itemView.findViewById<View?>("chat_list_adapter_item_text_decorator")
                reply?.let {
                    it.isClickable = false
                    it.setOnClickListener(null)
                }
            }
        }
    }

    fun patchMisc() {
        // disable reified function marker for viewmodel delegates
        patcher.instead<d0.z.d.m?>(
            "reifiedOperationMarker", Int::class.javaPrimitiveType!!, String::class.java
        ) {}
    }

    override fun stop(context: Context) = patcher.unpatchAll()
}
