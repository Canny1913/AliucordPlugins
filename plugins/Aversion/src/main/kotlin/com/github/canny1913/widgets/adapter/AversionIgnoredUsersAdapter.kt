package com.github.canny1913.widgets.adapter

import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Utils
import com.aliucord.wrappers.users.globalName
import com.discord.models.user.User
import com.discord.utilities.icon.IconUtils
import com.discord.utilities.mg_recycler.MGRecyclerAdapterSimple
import com.discord.utilities.mg_recycler.MGRecyclerDataPayload
import com.discord.utilities.mg_recycler.MGRecyclerViewHolder
import com.github.canny1913.R
import com.github.canny1913.databinding.AversionIgnoredUserItemBinding
import com.github.canny1913.viewmodel.AversionIgnoredUsersViewModel


class AversionIgnoredUsersAdapter(
    recyclerView: RecyclerView,
    var onClickUserProfile: (User) -> Unit,
    var onClickUnignore: (User) -> Unit,
) : MGRecyclerAdapterSimple<MGRecyclerDataPayload>(recyclerView, true) {
    class IgnoredUserItemHolder(adapter: AversionIgnoredUsersAdapter) :
        MGRecyclerViewHolder<AversionIgnoredUsersAdapter, MGRecyclerDataPayload?>(R.layout.widget_settings_ignored_user_item, adapter) {
        val binding = AversionIgnoredUserItemBinding(
            itemView as RelativeLayout,
            avatarView = itemView.findViewById(R.id.ignored_user_item_avatar)!!,
            usernameView = itemView.findViewById(R.id.ignored_user_item_name)!!,
            unignoreButtonView = itemView.findViewById(R.id.ignored_user_button)!!
        )

        override fun onConfigure(type: Int, data: MGRecyclerDataPayload?) {
            val item = data as? AversionIgnoredUsersViewModel.Item ?: return
            binding.rootView.setOnClickListener {
                adapter.onClickUserProfile(item.user)
            }
            IconUtils.setIcon(
                binding.avatarView,
                item.user,
                Utils.getResId("avatar_size_standard", "dimen"),
            )
            binding.usernameView.text = data.user.globalName ?: data.user.username
            binding.unignoreButtonView.setOnClickListener { adapter.onClickUnignore(data.user) }
        }
    }
    override fun onCreateViewHolder(
        root: ViewGroup, dataType: Int
    ): MGRecyclerViewHolder<*, MGRecyclerDataPayload?> {
        return when (dataType) {
            AversionIgnoredUsersViewModel.Item.TYPE -> IgnoredUserItemHolder(this)
            else -> throw invalidViewTypeException(dataType)
        }
    }
}
