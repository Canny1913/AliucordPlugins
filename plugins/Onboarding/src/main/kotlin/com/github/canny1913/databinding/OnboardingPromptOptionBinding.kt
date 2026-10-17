package com.github.canny1913.databinding

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBinding
import com.facebook.drawee.view.SimpleDraweeView

class OnboardingPromptOptionBinding(
    val rootView: ConstraintLayout,
    val clickedView: View,
    val emojiView: SimpleDraweeView,
    val optionTitleView: TextView,
    val optionDescriptionView: TextView,
) : ViewBinding {
    override fun getRoot(): View = rootView
}
