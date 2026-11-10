package com.github.canny1913

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.discord.api.activity.Activity
import com.discord.api.application.Application
import com.discord.api.channel.Channel
import com.discord.api.guildscheduledevent.GuildScheduledEvent
import com.discord.api.message.activity.MessageActivityType
import com.discord.api.message.reaction.MessageReaction
import com.discord.api.sticker.BaseSticker
import com.discord.api.sticker.Sticker
import com.discord.app.AppFragment
import com.discord.models.message.Message
import com.discord.restapi.RestAPIParams
import com.discord.stores.StoreChat
import com.discord.stores.StoreStream
import com.discord.utilities.mg_recycler.MGRecyclerAdapter
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemCallMessage
import com.github.canny1913.databinding.SearchPreviewFragmentBinding
import com.github.canny1913.fragment.fragmentBinding
import com.github.canny1913.viewmodel.SearchPreviewViewModel
import com.github.canny1913.viewmodel.SearchPreviewViewModel.Companion.loggerSubscribe
import com.github.canny1913.viewmodel.SearchPreviewViewModel.Companion.uiWithLifecycle
import com.github.canny1913.viewmodel.activityViewModels
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean


class SearchPreviewFragment(
    val ctx: Context, val channelId: Long, val messageId: Long
) : AppFragment(R.layout.widget_search_preview) {

    lateinit var searchAdapter: WidgetChatListAdapter
    val viewModel: SearchPreviewViewModel by activityViewModels {
        SearchPreviewViewModel.provideFactory(channelId, messageId)
    }
    val isScrolling: AtomicBoolean = AtomicBoolean(false)

    val binding: SearchPreviewFragmentBinding by fragmentBinding { rootView ->
        SearchPreviewFragmentBinding(
            rootView as ConstraintLayout,
            toolbarView = rootView.findViewById(R.id.action_bar_toolbar),
            recyclerView = rootView.findViewById(R.id.search_recycler),
            dimmerView = rootView.findViewById(R.id.dimmer_view),
            jumpButtonView = rootView.findViewById(R.id.search_jump),
        )
    }

    override fun getContext(): Context {
        return ctx
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("RestrictedApi")
    override fun getLayoutInflater(p0: Bundle?): LayoutInflater {
        return LayoutInflater.from(context)
    }

    override fun onCreateView(inflater: LayoutInflater, root: ViewGroup?, bundle: Bundle?): View? {
        return layoutInflater.inflate(R.layout.widget_search_preview, root, false)
    }

    override fun onViewBound(view: View?) {
        super.onViewBound(view)

        val recyclerView = binding.recyclerView

        searchAdapter = MGRecyclerAdapter.configure(
            WidgetChatListAdapter(
                recyclerView, this, parentFragmentManager, SearchPreviewEventHandler()
            ).apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
        )
        @Suppress("MISSING_DEPENDENCY_SUPERCLASS")
        searchAdapter.layoutManager.reverseLayout = true

        binding.jumpButtonView.setOnClickListener { viewModel.onJump(it, channelId, messageId) }
        binding.toolbarView.setNavigationOnClickListener { viewModel.onBackPress() }
        this.setOnBackPressed(viewModel::onBackPress)
    }

    fun configureUI(viewState: SearchPreviewViewModel.ViewState) {
        if (viewState is SearchPreviewViewModel.ViewState.Exit) {
            requireActivity().finish()
        }
        if (viewState is SearchPreviewViewModel.ViewState.Invalid) {
            Toast.makeText(ctx, R.string.load_fail, Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
        if (viewState is SearchPreviewViewModel.ViewState.Loading) {
            binding.jumpButtonView.visibility = View.GONE
            binding.dimmerView.setDimmed(true, true)
        }
        if (viewState is SearchPreviewViewModel.ViewState.Loaded) {
            binding.jumpButtonView.apply {
                this.setIsLoading(false)
                visibility = View.VISIBLE
                isEnabled = true
                isClickable = true
            }
            binding.dimmerView.setDimmed(false, false)
            searchAdapter.data = viewState.model
        }
    }

    override fun onViewBoundOrOnResume() {
        super.onViewBoundOrOnResume()
        searchAdapter.setHandlers()
        viewModel.viewStateSubject.uiWithLifecycle(this).loggerSubscribe { configureUI(it!!) }
        viewModel.scroller.uiWithLifecycle(this).loggerSubscribe {
            if (isScrolling.getAndSet(true)) return@loggerSubscribe

            searchAdapter.scrollToMessageId(it!!) { isScrolling.getAndSet(false) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        searchAdapter.dispose()
    }

    override fun onPause() {
        super.onPause()
        searchAdapter.disposeHandlers()
    }

    class SearchPreviewEventHandler : WidgetChatListAdapter.EventHandler {
        override fun onBotUiComponentClicked(
            p0: Long,
            p1: Long?,
            p2: Long,
            p3: Long,
            p4: Long?,
            p5: Int,
            p6: RestAPIParams.ComponentInteractionData?
        ) {
        }

        override fun onCallMessageClicked(
            p0: Long,
            p1: WidgetChatListAdapterItemCallMessage.CallStatus?
        ) {
        }

        override fun onCommandClicked(
            p0: Long,
            p1: Long?,
            p2: Long,
            p3: Long,
            p4: Long,
            p5: Long,
            p6: String?
        ) {
        }

        override fun onDismissClicked(p0: Message?) {}
        override fun onDismissLocalMessageClicked(p0: Message?) {}
        override fun onInteractionStateUpdated(p0: StoreChat.InteractionState?) {}
        override fun onListClicked() {}
        override fun onMessageAuthorAvatarClicked(p0: Message?, p1: Long) {}
        override fun onMessageAuthorLongClicked(p0: Message?, p1: Long?) {}
        override fun onMessageAuthorNameClicked(p0: Message?, p1: Long) {}
        override fun onMessageBlockedGroupClicked(p0: Message) {
            StoreStream.Companion!!.chat.toggleBlockedMessageGroup(p0.id)
        }

        override fun onMessageClicked(p0: Message?, p1: Boolean) {}
        override fun onMessageLongClicked(p0: Message?, p1: CharSequence?, p2: Boolean) {}
        override fun onOldestMessageId(p0: Long, p1: Long) {}
        override fun onOpenPinsClicked(p0: Message?) {}
        override fun onQuickAddReactionClicked(p0: Long, p1: Long, p2: Long, p3: Long) {}
        override fun onQuickDownloadClicked(p0: Uri?, p1: String?): Boolean = true
        override fun onReactionClicked(
            p0: Long,
            p1: Long,
            p2: Long,
            p3: Long,
            p4: MessageReaction?,
            p5: Boolean
        ) {
        }

        override fun onReactionLongClicked(p0: Long, p1: Long, p2: Long, p3: MessageReaction?) {}
        override fun onReportIssueWithAutoMod(p0: Context?, p1: Message?) {}
        override fun onRoleSubscriptionPurchaseTierClick(
            p0: Long,
            p1: Long,
            p2: Long,
            p3: Long,
            p4: Long
        ) {
        }

        override fun onSendGreetMessageClicked(p0: Long, p1: Int, p2: Sticker?) {}
        override fun onShareButtonClick(
            p0: GuildScheduledEvent?,
            p1: WeakReference<Context?>?,
            p2: WeakReference<AppFragment?>?
        ) {
        }

        override fun onStickerClicked(p0: Message?, p1: BaseSticker?) {}
        override fun onSystemMessageCtaClicked(p0: Message?, p1: Channel?, p2: BaseSticker?) {}
        override fun onThreadClicked(p0: Channel?) {}
        override fun onThreadLongClicked(p0: Channel?) {}
        override fun onUrlLongClicked(p0: String?) {}
        override fun onUserActivityAction(
            p0: Long,
            p1: Long,
            p2: Long,
            p3: MessageActivityType?,
            p4: Activity?,
            p5: Application?
        ) {
        }

        override fun onUserMentionClicked(p0: Long, p1: Long, p2: Long) {}
    }
}