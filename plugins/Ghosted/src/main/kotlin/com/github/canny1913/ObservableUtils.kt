package com.github.canny1913

import com.aliucord.Logger
import com.aliucord.utils.RxUtils
import com.aliucord.utils.RxUtils.subscribe
import com.discord.app.AppComponent
import com.discord.utilities.mg_recycler.MGRecyclerAdapterSimple
import com.discord.utilities.rx.ObservableExtensionsKt
import rx.Observable

object ObservableUtils {
    private val logger = Logger("GhostedObservable")
    fun <T> Observable<T>.uiWithLifecycle(
        component: AppComponent? = null, adapter: MGRecyclerAdapterSimple<*>? = null
    ): Observable<T> = ObservableExtensionsKt.ui(this, component, adapter)

    fun <T> Observable<T>.withComputation(): Observable<T> =
        ObservableExtensionsKt.computationLatest(this)

    fun <T> Observable<T>.loggerSubscribe(
        onError: (Throwable) -> Unit = {}, onCompleted: () -> Unit = {}, onNext: (T) -> Unit
    ) = RxUtils.createActionSubscriber(
        onNext, onError = {
            logger.error(it)
            onError(it)
        }, onCompleted
    ).apply { subscribe(this) }
    fun <T> Observable<T>.ui() = ObservableExtensionsKt.ui(this)!!
    fun <T, R> Observable<T>.switch(map: (T) -> Observable<out R>) = Y(map)!!
    fun <T1, T2, R> combineLatest(first: Observable<out T1>, second: Observable<out T2>, func: (T1, T2) -> R) = Observable.j(first, second, func)!!

}