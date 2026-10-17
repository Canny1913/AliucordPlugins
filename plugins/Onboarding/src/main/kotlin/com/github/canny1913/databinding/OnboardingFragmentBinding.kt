package com.github.canny1913.databinding

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.discord.utilities.dimmer.DimmerView
import com.discord.views.LoadingButton

class OnboardingFragmentBinding(
    val rootView: ConstraintLayout,
    val toolbarView: Toolbar,
    val progressBarView: ProgressBar,
    val recyclerView: RecyclerView,
    val scrollView: NestedScrollView,
    val dimmerView: DimmerView,
    val nextGradientView: View,
    val roleTextView: TextView,
    val nextButtonView: LoadingButton
) : ViewBinding {
    override fun getRoot(): View = rootView
}
