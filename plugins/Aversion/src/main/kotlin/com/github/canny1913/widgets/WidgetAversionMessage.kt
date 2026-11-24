package com.github.canny1913.widgets

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Utils
import com.aliucord.utils.ReflectUtils
import com.aliucord.wrappers.users.globalName
import com.discord.stores.StoreStream
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter
import com.discord.widgets.chat.list.adapter.WidgetChatListItem
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.github.canny1913.Aversion
import com.github.canny1913.R
import com.github.canny1913.databinding.WidgetAversionMessageBinding
import com.github.canny1913.entries.AversionBlockedEntry
import com.github.canny1913.entries.AversionEntry
import com.github.canny1913.entries.AversionIgnoredEntry

@SuppressLint("UseKtx")
class WidgetAversionMessage(adapter: WidgetChatListAdapter) :
    WidgetChatListItem(Utils.getResId("widget_chat_list_adapter_item_minimal", "layout"), adapter) {
    val blockedBackground = ColorDrawable(Aversion.blockedColor).apply { mutate() }
    val defaultBackground: Drawable? = itemView.background
    val tapGestureListener by lazy {
        GestureDetector(itemView.context, object :
            GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                adapter.eventHandler.onListClicked()
                return super.onSingleTapUp(e)
            }
        })
    }
    init {
        // todo: ACCESS FIELD SHOULD HANDLE INHERITED FIELDS COME ON
        val newView = LayoutInflater.from(Aversion.pluginCtx).inflate(R.layout.widget_aversion_blocked_message, adapter.recycler, false)
        ReflectUtils.setFinalField(RecyclerView.ViewHolder::class.java, this, "itemView", newView)

        @SuppressLint("ClickableViewAccessibility")
        itemView.setOnTouchListener { _, event ->
            tapGestureListener.onTouchEvent(event)
        }
        itemView.background = blockedBackground
    }
    val binding: WidgetAversionMessageBinding by lazy {
        WidgetAversionMessageBinding(
            rootView = itemView as ConstraintLayout,
            textView = itemView.findViewById(R.id.item_text),
            iconView = itemView.findViewById(R.id.item_icon),
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onConfigure(p0: Int, p1: ChatListEntry) {
        val data = p1 as AversionEntry

        if (data.hidden) {
            itemView.layoutParams.run {
                height = 0
                width = 0
            }
            itemView.visibility = View.GONE
        } else {
            itemView.layoutParams.run {
                height = RecyclerView.LayoutParams.WRAP_CONTENT
                width = RecyclerView.LayoutParams.MATCH_PARENT
            }
            itemView.visibility = View.VISIBLE
        }
        itemView.background = if (data.isExpanded) blockedBackground else defaultBackground
        val authors = data.authors.map { it.globalName ?: it.username }
        val pluralsId = when (data) {
            is AversionBlockedEntry -> R.plurals.blocked_messages_count
            is AversionIgnoredEntry -> R.plurals.ignored_messages_count
            else -> error("Bad entry!")
        }
        val quantityStr = itemView.context.resources.getQuantityString(pluralsId, data.aversionCount, data.aversionCount, authors.joinToString(separator = ", "))
        val spannableString = SpannableStringBuilder(quantityStr)
        // apply bold styling to role names
        authors.forEach { author ->
            val start = quantityStr.indexOf(author)
            val end = start + author.length
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.textView.text = spannableString
        val drawableId = when (data) {
            is AversionBlockedEntry -> R.drawable.ic_blocked
            is AversionIgnoredEntry -> R.drawable.ic_ignored
            else -> error("Bad entry!")
        }
        binding.iconView.setImageResource(drawableId)
        binding.rootView.setOnClickListener {
            if (data.isExpanded) {
                Aversion.expandedIds.removeAll(data.messageIds)
            } else {
                Aversion.expandedIds.addAll(data.messageIds)
            }
            // Still toggle in normal store so chat refreshes
            StoreStream.getChat().toggleBlockedMessageGroup(data.messageIds.first())
        }
    }
}