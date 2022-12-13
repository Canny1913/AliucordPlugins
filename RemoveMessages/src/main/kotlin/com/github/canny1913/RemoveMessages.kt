package com.github.canny1913

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.discord.models.domain.ModelMessageDelete
import com.discord.stores.StoreStream
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.entries.MessageEntry

@AliucordPlugin(requiresRestart = false)
class RemoveMessages : Plugin() {
    init {
        settingsTab = SettingsTab(PluginSettings::class.java).withArgs(settings)
    }
    override fun start(context: Context) {
        patcher.after<WidgetChatListAdapterItemMessage>(
            "onConfigure",
            Int::class.java,
            ChatListEntry::class.java
        ) { param ->

            val entry = param.args[1] as MessageEntry

            if (entry.message.isLoading) return@after

            if (entry.message.author.id == settings.getLong("id", 0L)) {
                StoreStream.getMessages().handleMessageDelete(
                    ModelMessageDelete(
                        entry.message.channelId,
                        entry.message.id
                    )
                )
            }
        }
    }

    override fun stop(context: Context) {
        // Remove all patches
        patcher.unpatchAll()
    }
}
