package com.github.canny1913.viewmodel

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aliucord.Logger
import com.aliucord.utils.RxUtils
import com.aliucord.utils.RxUtils.map
import com.aliucord.utils.RxUtils.subscribe
import com.aliucord.wrappers.ChannelWrapper.Companion.guildId
import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.discord.api.channel.Channel
import com.discord.app.AppComponent
import com.discord.models.message.Message
import com.discord.stores.StoreMessagesLoader
import com.discord.stores.StoreStream
import com.discord.utilities.embed.InviteEmbedModel
import com.discord.utilities.mg_recycler.MGRecyclerAdapterSimple
import com.discord.utilities.rest.RestAPI
import com.discord.utilities.rx.ObservableCombineLatestOverloadsKt
import com.discord.utilities.rx.ObservableExtensionsKt
import com.discord.widgets.botuikit.ComponentChatListState
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.model.WidgetChatListModel
import com.discord.widgets.chat.list.model.WidgetChatListModelMessages
import com.discord.widgets.chat.list.model.`WidgetChatListModelMessages$Companion$get$1`
import rx.Observable
import rx.subjects.BehaviorSubject


class SearchPreviewViewModel(
    val channelId: Long, val messageId: Long
) : DiscordViewModel<SearchPreviewViewModel.ViewState>(ViewState.Loading()) {
    val scroller: BehaviorSubject<Long> = BehaviorSubject.k0()

    init {
        RestAPI.getApi().getChannelMessagesAround(channelId, 50, messageId).map {
            StoreMessagesLoader.ChannelChunk(
                channelId, it.map(::Message), false, false, false, false
            )
        }.withComputation().flatMap(::observeStores).loggerSubscribe(onNext = ::handleStoreState)
    }

    fun onJump(view: View, channelId: Long, messageId: Long) {
        StoreStream.Companion!!.messagesLoader.jumpToMessage(channelId, messageId)
        b.a.d.j.c(view.context, false, null, 6)
    }

    fun onBackPress(): Boolean {
        updateViewState(ViewState.Exit())
        return true
    }

    private fun handleStoreState(state: StoreState) {
        if (state is StoreState.Loaded) {
            StoreStream.Companion!!.messagesLoader.clearScrollTo()
            // else normal chat views will also get affected by message loader
            val model = WidgetChatListModel(
                state.userId,
                state.channel.id,
                null,
                0,
                emptyMap(),
                -1,
                state.messages,
                state.roles,
                0,
                0,
                false
            )
            // todo: ehhh
            if (getViewState() is ViewState.Loading) {
                scroller.onNext(messageId)
            }
            updateViewState(
                ViewState.Loaded(
                    model, state.channel
                )
            )
        } else {
            updateViewState(ViewState.Invalid())
        }
    }

    sealed interface ViewState {
        class Invalid : ViewState
        class Loading : ViewState
        class Exit : ViewState
        class Loaded(
            val model: WidgetChatListModel, val channel: Channel
        ) : ViewState
    }

    sealed interface StoreState {
        class Loaded(
            val messages: List<ChatListEntry>,
            val userId: Long,
            val roles: Set<Long>,
            val channel: Channel
        ) : StoreState
    }

    private fun observeStores(
        chunk: StoreMessagesLoader.ChannelChunk
    ): Observable<StoreState> {
        val storeStream = StoreStream.Companion
        // todo: nope
        val guildId = storeStream.channelsSelected.selectedChannel.guildId
        // notify stores to update guild members post-search
        storeStream.dispatcherYesThisIsIntentional.schedule {
            storeStream.guildMemberRequester.handleLoadMessages(channelId, chunk.messages)
            storeStream.users.handleMessagesLoaded(chunk)
            storeStream.repliedMessages.handleLoadMessages(chunk.messages)
        }

        val messagesWithMetadataObservable = Observable.j(
            WidgetChatListModelMessages.MessagesWithMetadata.Companion!!.get(chunk.messages.reversed()),
            storeStream.repliedMessages.observeMessageReferencesForChannel(chunk.channelId)
        ) { metadata, repliedState ->
            WidgetChatListModelMessages.MessagesWithMetadata(
                metadata.messages,
                metadata.messageState,
                metadata.messageThreads,
                metadata.threadCountsAndLatestMessages,
                repliedState,
                emptyMap()
            )
        }
        // YESSSS MORE OBSERVINGGGGG
        return ObservableCombineLatestOverloadsKt.combineLatest(
            messagesWithMetadataObservable,
            storeStream.channels.observeChannel(channelId),
            storeStream.guilds.observeRoles(guildId),
            storeStream.guilds.observeComputed(guildId),
            storeStream.userRelationships.observeForType(2),
            storeStream.chat.observeExpandedBlockedMessageIds(),
            storeStream.userSettings.observeIsAutoPlayGifsEnabled(false),
            storeStream.userSettings.observeIsAnimatedEmojisEnabled(false),
            storeStream.userSettings.observeIsRenderEmbedsEnabled(),
            ComponentChatListState.INSTANCE.observeChatListComponentState(),
            InviteEmbedModel.Companion.`observe$default`(
                InviteEmbedModel.Companion!!, null, null, null, null, 15, null
            )
        ) { messagesWithMetadata, channel, roles, guildMembers, blockedIds, blockedExpandedIds, autoPlay, animated, renderEmbeds, componentState, inviteEmbed ->
            val me = storeStream.users.me.id
            val model = `WidgetChatListModelMessages$Companion$get$1`(
                channel
            ).invoke(
                messagesWithMetadata,
                channel,
                blockedIds,
                blockedExpandedIds,
                guildMembers,
                0L, //new message marker
                roles,
                null,
                animated,
                autoPlay,
                renderEmbeds,
                me,
                componentState,
                inviteEmbed as InviteEmbedModel,
                false
            )
            return@combineLatest StoreState.Loaded(
                model.items, me, guildMembers[me]?.roles?.toSet() ?: emptySet(), channel
            )

        }

    }

    companion object {
        private val logger = Logger("SearchPreviewVM")

        fun provideFactory(
            channelId: Long, messageId: Long
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
            ): T {
                return SearchPreviewViewModel(channelId, messageId) as T
            }
        }

        fun <T> Observable<T>.uiWithLifecycle(
            component: AppComponent? = null, adapter: MGRecyclerAdapterSimple<*>? = null
        ): Observable<T> = ObservableExtensionsKt.ui(this, component, adapter)

        fun <T> Observable<T>.withComputation(): Observable<T> =
            ObservableExtensionsKt.computationLatest(this)

        fun <T> Observable<T>.loggerSubscribe(
            onError: (Throwable) -> Unit = {}, onCompleted: () -> Unit = {}, onNext: (T) -> Unit
        ) = RxUtils.createActionSubscriber(
            onNext, onError = {
                logger.error(it)
                onError(it)
            }, onCompleted
        ).apply { subscribe(this) }

        fun <T, R> Observable<T>.flatMap(map: (T) -> Observable<out R>) = A(map)!!
    }
}

