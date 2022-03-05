package com.krystianwsul.checkme.domainmodel

import androidx.annotation.CheckResult
import com.mindorks.scheduler.Priority
import com.mindorks.scheduler.RxPS
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

fun getDomainScheduler(priority: Priority? = null) = RxPS.get(priority ?: Priority.MEDIUM)

fun <T : Any> Observable<T>.observeOnDomain(priority: Priority? = null) = observeOn(getDomainScheduler(priority))

fun <T : Any> Single<T>.observeOnDomain(priority: Priority? = null) = observeOn(getDomainScheduler(priority))
fun <T : Any> Flowable<T>.observeOnDomain(priority: Priority? = null) = observeOn(getDomainScheduler(priority))

fun <T : Any> Observable<T>.subscribeOnDomain(priority: Priority? = null) = subscribeOn(getDomainScheduler(priority))
fun <T : Any> Single<T>.subscribeOnDomain(priority: Priority? = null) = subscribeOn(getDomainScheduler(priority))
fun Completable.subscribeOnDomain(priority: Priority? = null) = subscribeOn(getDomainScheduler(priority))

@CheckResult
fun completeOnDomain(priority: Priority, action: () -> Unit) = Completable.fromAction(action).subscribeOnDomain(priority)

fun runOnDomain(priority: Priority, action: () -> Unit) = completeOnDomain(priority, action).subscribe()

fun <T : Any> Observable<T>.observeOnDomainSplitPriorities(initialPriority: Priority, laterPriority: Priority? = null) =
    // todo scheduling
    take(1).observeOnDomain(initialPriority)
        .concatWith(skip(1).observeOnDomain(laterPriority))