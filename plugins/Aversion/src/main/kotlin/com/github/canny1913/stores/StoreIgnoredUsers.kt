package com.github.canny1913.stores

import android.annotation.SuppressLint
import com.aliucord.Utils
import com.discord.utilities.persister.Persister
import com.github.canny1913.ObservableUtils.withComputation
import com.github.canny1913.models.PartialReadyEvent
import com.github.canny1913.models.UserRelationshipEvent
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import java.util.Collections

object StoreIgnoredUsers {
    private val UNLOADED_IDS_SENTINEL = hashSetOf<Long>()
    @SuppressLint("StaticFieldLeak")
    private val ignoredCache: Persister<HashSet<Long>> = Persister("STORE_IGNORED_USERS_V126", UNLOADED_IDS_SENTINEL)
    private var subject = SerializedSubject(BehaviorSubject.l0(setOf<Long>()))
    private var gatewayUpdateSubject = PublishSubject.k0<UserRelationshipEvent>()

    var ignoredIdsSnapshot: MutableSet<Long> = Collections.synchronizedSet(hashSetOf<Long>())

    val ignoredIds: Set<Long> get() = ignoredIdsSnapshot

    fun observe(): Observable<Set<Long>> {
        return subject.withComputation()
    }
    fun observeUpdates() : Observable<UserRelationshipEvent> {
        return gatewayUpdateSubject
    }

    fun notify(data: Set<Long>) {
        Utils.threadPool.execute { synchronized(ignoredIdsSnapshot) { snapshotData() } }
        subject.k.onNext(data)
    }

    fun handleConnectionOpen(payload: PartialReadyEvent) {
        val relationships = payload.relationships
        val hashSet = relationships.filter { it.userIgnored && it.type != 2 }.map { it.userId ?: it.id }.toHashSet()
        ignoredIdsSnapshot.clear()
        ignoredIdsSnapshot.addAll(hashSet)
        notify(ignoredIdsSnapshot)
    }

    fun handlePreLogout() {
        ignoredIdsSnapshot.clear()
        subject.onNext(ignoredIdsSnapshot)
    }

    fun handleRelationshipUpdate(relationship: UserRelationshipEvent) {
        val set = ignoredIdsSnapshot
        if (relationship.type == 2) {
            set.remove(relationship.id)
        } else if (relationship.userIgnored) {
            set.add(relationship.id)
        } else {
            set.remove(relationship.id)
        }
        notify(set)
        gatewayUpdateSubject.onNext(relationship)
    }

    init {
        Utils.threadPool.execute {
            val set = this.ignoredCache.get()
            if (set != UNLOADED_IDS_SENTINEL) {
                ignoredIdsSnapshot.clear()
                ignoredIdsSnapshot.addAll(set)
            }
        }
    }



    fun snapshotData() {
        if (ignoredIdsSnapshot.isNotEmpty()) {
            val set = HashSet(ignoredIdsSnapshot)
            Persister.`set$default`(this.ignoredCache, set, true, 2, null)
            this.ignoredIdsSnapshot.clear()
            this.ignoredIdsSnapshot.addAll(set)
        } else {
            Persister.`clear$default`(this.ignoredCache, true, 1, null)
            this.ignoredIdsSnapshot.clear()
        }
    }
}