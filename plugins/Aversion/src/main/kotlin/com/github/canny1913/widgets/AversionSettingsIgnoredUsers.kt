package com.github.canny1913.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.aliucord.Logger
import com.discord.app.AppFragment
import com.discord.models.user.User
import com.discord.utilities.mg_recycler.MGRecyclerAdapter
import com.discord.widgets.user.usersheet.WidgetUserSheet
import com.github.canny1913.Aversion
import com.github.canny1913.ObservableUtils.loggerSubscribe
import com.github.canny1913.ObservableUtils.uiWithLifecycle
import com.github.canny1913.R
import com.github.canny1913.databinding.AversionSettingsIgnoredUsersBinding
import com.github.canny1913.fragment.fragmentBinding
import com.github.canny1913.viewmodel.AversionIgnoredUsersViewModel
import com.github.canny1913.viewmodel.AversionIgnoredUsersViewModel.ViewState
import com.github.canny1913.viewmodel.activityViewModels
import com.github.canny1913.widgets.adapter.AversionIgnoredUsersAdapter


class AversionSettingsIgnoredUsers : AppFragment(R.layout.aversion_settings_ignored_users) {
    val logger = Logger("AversionSettings")

    val binding by fragmentBinding { rootView ->
        AversionSettingsIgnoredUsersBinding(
            rootView as CoordinatorLayout,
            flipperView = rootView.findViewById(R.id.ignored_users_flipper),
            toolbarView = rootView.findViewById(R.id.action_bar_toolbar),
            recyclerView = rootView.findViewById(R.id.ignored_users_list_recycler)
        )
    }
    val viewModel: AversionIgnoredUsersViewModel by activityViewModels(AversionIgnoredUsersViewModel::provideFactory)
    lateinit var adapter: AversionIgnoredUsersAdapter

    override fun getContext(): Context {
        return Aversion.pluginCtx!!
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("RestrictedApi")
    override fun getLayoutInflater(p0: Bundle?): LayoutInflater {
        return LayoutInflater.from(context)
    }

    override fun onCreateView(inflater: LayoutInflater, root: ViewGroup?, bundle: Bundle?): View? {
        return layoutInflater.inflate(R.layout.aversion_settings_ignored_users, root, false)
    }

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        appActivity.n(binding.toolbarView)
        setActionBarTitle(R.string.user_settings_ignored_users)
        `setActionBarDisplayHomeAsUpEnabled$default`(this, false, 1, null)
        adapter = MGRecyclerAdapter.configure(AversionIgnoredUsersAdapter(binding.recyclerView, ::onClickUserProfile, ::onClickUnblock))
    }

    fun configureUI(state: ViewState) {
        when (state) {
            is ViewState.Empty -> binding.flipperView.displayedChild = 1
            is ViewState.Loaded -> configureLoadedUI(state)
            is ViewState.Loading -> {}
        }
    }

    fun configureLoadedUI(state: ViewState.Loaded) {
        binding.flipperView.displayedChild = 0
        adapter.setData(state.items)
    }

    fun onClickUserProfile(user: User) {
        WidgetUserSheet.Companion!!.show(user.id, null, parentFragmentManager)
    }
    fun onClickUnblock(user: User) {
        viewModel.onClickUnignore(user) {
            val err = it ?: return@onClickUnignore
            logger.error("Failed to unignore", err)
            logger.errorToast(getString(R.string.failed_to_unignore), err)
        }
    }
    override fun onViewBoundOrOnResume() {
        super.onViewBoundOrOnResume()
        viewModel.viewStateSubject.uiWithLifecycle(this).loggerSubscribe(onNext = ::configureUI)
    }
}