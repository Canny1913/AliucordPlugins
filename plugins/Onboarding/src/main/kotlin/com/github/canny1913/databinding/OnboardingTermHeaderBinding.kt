package com.github.canny1913.databinding

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewbinding.ViewBinding

class OnboardingTermHeaderBinding(
    val rootView: LinearLayout,
    val titleView: TextView,
    val descView: TextView,
) : ViewBinding {
    override fun getRoot(): View = rootView
}
