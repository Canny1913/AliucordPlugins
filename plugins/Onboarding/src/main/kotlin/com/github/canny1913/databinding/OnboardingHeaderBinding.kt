package com.github.canny1913.databinding

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewbinding.ViewBinding

class OnboardingHeaderBinding(
    val rootView: LinearLayout,
    val indexView: TextView,
    val dotView: TextView,
    val requiredView: TextView,
    val textView: TextView,
) : ViewBinding {
    override fun getRoot(): View = rootView
}
