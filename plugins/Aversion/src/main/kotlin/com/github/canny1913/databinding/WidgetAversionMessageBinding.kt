package com.github.canny1913.databinding

import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBinding
import com.discord.utilities.view.text.LinkifiedTextView

class WidgetAversionMessageBinding(
    val rootView: ConstraintLayout,
    val iconView: ImageView,
    val textView: LinkifiedTextView,
) : ViewBinding {
    override fun getRoot(): View = rootView
}