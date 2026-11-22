package com.github.canny1913.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.aliucord.Utils
import com.discord.app.AppFragment
import com.discord.stores.StoreStream
import com.discord.widgets.chat.list.model.WidgetChatListModelMessages
import com.github.canny1913.Aversion
import com.github.canny1913.ObservableUtils.loggerSubscribe
import com.github.canny1913.ObservableUtils.uiWithLifecycle
import com.github.canny1913.ObservableUtils.withComputation
import com.github.canny1913.R
import com.github.canny1913.databinding.WidgetChatIndicatorBinding
import com.github.canny1913.entries.AversionEntry
import com.github.canny1913.fragment.fragmentBinding
import com.github.canny1913.settings.AversionSettings
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class WidgetChatIndicator : AppFragment(R.layout.aversion_chat_indicator) {

    val dismissed = AtomicBoolean(false)
    val binding: WidgetChatIndicatorBinding by fragmentBinding { rootView ->
        WidgetChatIndicatorBinding(
            rootView as LinearLayout,
            iconView = rootView.findViewById(R.id.aversion_chat_indicator_dismiss),
            textView = rootView.findViewById(R.id.aversion_chat_indicator_text)
        )
    }

    override fun getContext(): Context {
        return Aversion.pluginCtx!!
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("RestrictedApi")
    override fun getLayoutInflater(p0: Bundle?): LayoutInflater {
        return LayoutInflater.from(context)
    }

    override fun onCreateView(inflater: LayoutInflater, root: ViewGroup?, bundle: Bundle?): View? {
        return layoutInflater.inflate(R.layout.aversion_chat_indicator, root, false)
    }

    fun configureUI(model: WidgetChatListModelMessages) {
        val authorCount = model.items.filterIsInstance<AversionEntry>().flatMap { it.authors }.distinctBy { it.id }.count()

        if (authorCount == 0 || dismissed.get() || !AversionSettings.enableIndicator) {
            binding.root.visibility = View.GONE
            return
        }
        binding.root.visibility = View.VISIBLE
        val quantityStr = context.resources.getQuantityString(R.plurals.chat_indicator_text, authorCount, authorCount)
        binding.textView.text = quantityStr
        binding.iconView.setOnClickListener {
            dismissed.set(true)
            binding.root.visibility = View.GONE
        }
    }
    override fun onViewBoundOrOnResume() {
        super.onViewBoundOrOnResume()
        StoreStream.Companion!!.channelsSelected.observeSelectedChannel().r().Y {
            dismissed.set(false)
            WidgetChatListModelMessages.Companion!!.get(it).p(200L, TimeUnit.MILLISECONDS)
        }.uiWithLifecycle(this).withComputation().loggerSubscribe { Utils.mainThread.post { configureUI(it) } }
    }
}