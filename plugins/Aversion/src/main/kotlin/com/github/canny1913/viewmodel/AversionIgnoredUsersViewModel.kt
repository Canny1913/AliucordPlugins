package com.github.canny1913.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aliucord.utils.RxUtils.map
import com.aliucord.wrappers.users.globalName
import com.discord.models.user.User
import com.discord.stores.StoreStream
import com.discord.utilities.mg_recycler.MGRecyclerDataPayload
import com.github.canny1913.Aversion
import com.github.canny1913.ObservableUtils.loggerSubscribe
import com.github.canny1913.ObservableUtils.switch
import com.github.canny1913.ObservableUtils.uiWithLifecycle
import com.github.canny1913.stores.StoreIgnoredUsers


class AversionIgnoredUsersViewModel : DiscordViewModel<AversionIgnoredUsersViewModel.ViewState>(ViewState.Loading) {
    init {
        observeStores()
    }

    fun onClickUnignore(user: User, callback: (Throwable?) -> Unit) {
        Aversion.sendIgnoreRequest(user.id, "DELETE", Aversion.SETTINGS_UNIGNORE_LOCATION) {
            callback(it)
        }
    }

    private fun handleStoreState(state: StoreState) {
        if (state is StoreState.Loaded && state.users.isNotEmpty()) {
            val sortedItems = state.users.values.map(::Item).sortedWith { a, b -> compareValuesBy(
                a, b,
                { (it.user.globalName ?: it.user.username).lowercase() },
                { it.user.username.lowercase() },
                { it.user.discriminator },
            ) }
            updateViewState(
                ViewState.Loaded(
                    sortedItems
                )
            )
        } else {
            updateViewState(ViewState.Empty)
        }
    }

    fun observeStores() {
        val storeStream = StoreStream.Companion!!
        StoreIgnoredUsers.observe().switch { set ->
            storeStream.users.observeUsers(set).map(StoreState::Loaded)
        }.uiWithLifecycle(this).loggerSubscribe(onNext = ::handleStoreState)
    }

    sealed interface ViewState {
        object Empty : ViewState
        object Loading : ViewState
        class Loaded(
            val items: List<Item>
        ) : ViewState
    }

    sealed interface StoreState {
        class Loaded(
            val users: Map<Long, User>
        ) : StoreState
    }

    class Item(
        val user: User
    ): MGRecyclerDataPayload {
        override fun getKey(): String = user.id.toString()

        override fun getType(): Int = TYPE
        companion object {
            const val TYPE = 70
        }
    }
    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
            ): T {
                return AversionIgnoredUsersViewModel() as T
            }
        }
    }
}

