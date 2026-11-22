package com.github.canny1913.databinding

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewbinding.ViewBinding

class WidgetChatIndicatorBinding(
    val rootView: LinearLayout,
    val iconView: ImageView,
    val textView: TextView,
) : ViewBinding {
    override fun getRoot(): View = rootView
}