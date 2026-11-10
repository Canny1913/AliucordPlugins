package com.github.canny1913.databinding

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.discord.utilities.dimmer.DimmerView
import com.discord.views.LoadingButton

class SearchPreviewFragmentBinding(
    val rootView: ConstraintLayout,
    val toolbarView: Toolbar,
    val recyclerView: RecyclerView,
    val dimmerView: DimmerView,
    val jumpButtonView: LoadingButton
) : ViewBinding {
    override fun getRoot(): View = rootView
}
