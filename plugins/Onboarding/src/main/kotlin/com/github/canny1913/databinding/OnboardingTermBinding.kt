package com.github.canny1913.databinding

import android.view.View
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.discord.utilities.view.rounded.RoundedRelativeLayout
import com.discord.utilities.view.text.SimpleDraweeSpanTextView


class OnboardingTermBinding(
    val rootView: RoundedRelativeLayout,
    val termView: SimpleDraweeSpanTextView,
    val dividerView: View,
    val indexView: TextView,
) : ViewBinding {

    override fun getRoot(): View = rootView
}
