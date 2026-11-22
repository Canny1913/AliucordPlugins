package com.github.canny1913.viewmodel

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentViewModelLazyKt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.discord.app.AppComponent
import rx.subjects.BehaviorSubject
import rx.subjects.Subject

/**
 * A re-implementation of Discord's [ViewModel] extension.
 */
abstract class DiscordViewModel<V>: AppComponent, ViewModel {
    @JvmField
    val unsubscribeSignal: Subject<Void?, Void?> = BehaviorSubject.k0()
    val viewStateSubject: BehaviorSubject<V> = BehaviorSubject.k0()

    constructor(viewState: V?) {
        viewStateSubject.onNext(viewState)
    }

    /**
     * Constructs a view model with no initial state.
     */
    constructor(): this(null)

    /**
     * Returns the current view state.
     *
     * @return Current view state. `null` if no view state is present
     */
    fun getViewState(): V? = viewStateSubject.n0()

    /**
     * Updates the current view state and emits it to observers.
     *
     * This method **must be called on the main (UI) thread**.
     *
     * @param newState The new view state that should replace the current state.
     *
     * @throws IllegalStateException if the method is called from a thread other
     * than the main thread.
     */
    @MainThread
    fun updateViewState(newState: V) {
        viewStateSubject.onNext(newState)
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribeSignal.onNext(null)
    }

    override fun getUnsubscribeSignal(): Subject<Void?, Void?> = unsubscribeSignal
}
fun <VM: ViewModel> Fragment.activityViewModels(factory: () -> (ViewModelProvider.Factory)): Lazy<VM> = FragmentViewModelLazyKt.activityViewModels<VM>(this, factory)
