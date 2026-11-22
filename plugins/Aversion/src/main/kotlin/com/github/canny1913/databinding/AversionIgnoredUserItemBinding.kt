package com.github.canny1913.databinding

import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.button.MaterialButton

class AversionIgnoredUserItemBinding(
    val rootView: RelativeLayout,
    val avatarView: SimpleDraweeView,
    val usernameView: TextView,
    val unignoreButtonView: MaterialButton
) : ViewBinding {
    override fun getRoot(): View = rootView
}