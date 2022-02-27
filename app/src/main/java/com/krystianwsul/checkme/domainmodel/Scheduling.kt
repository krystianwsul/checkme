package com.krystianwsul.checkme.domainmodel

import androidx.annotation.CheckResult
import com.mindorks.scheduler.Priority
import com.mindorks.scheduler.RxPS
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

private fun getScheduler(priority: Priority? = null) = RxPS.get(priority ?: Priority.MEDIUM)

fun <T : Any> Observable<T>.observeOnDomain(priority: Priority? = null) = observeOn(getScheduler(priority))
fun <T : Any> Single<T>.observeOnDomain(priority: Priority? = null) = observeOn(getScheduler(priority))
fun <T : Any> Flowable<T>.observeOnDomain(priority: Priority? = null) = observeOn(getScheduler(priority))

fun <T : Any> Observable<T>.subscribeOnDomain(priority: Priority? = null) = subscribeOn(getScheduler(priority))
fun <T : Any> Single<T>.subscribeOnDomain(priority: Priority? = null) = subscribeOn(getScheduler(priority))
fun Completable.subscribeOnDomain(priority: Priority? = null) = subscribeOn(getScheduler(priority))

@CheckResult
fun completeOnDomain(action: () -> Unit) = Completable.fromAction(action).subscribeOnDomain()

fun runOnDomain(action: () -> Unit) = completeOnDomain(action).subscribe()

fun <T : Any> Observable<T>.observeOnDomainSplitPriorities(initialPriority: Priority, laterPriority: Priority? = null) =
    // todo scheduling
    take(1).observeOnDomain(initialPriority)
        .concatWith(skip(1).observeOnDomain(laterPriority))