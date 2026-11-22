package com.github.canny1913.databinding

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.discord.app.AppViewFlipper

class AversionSettingsIgnoredUsersBinding(
    val rootView: CoordinatorLayout,
    val toolbarView: Toolbar,
    val flipperView: AppViewFlipper,
    val recyclerView: RecyclerView,
) : ViewBinding {
    override fun getRoot(): View = rootView
}