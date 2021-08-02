package com.krystianwsul.checkme.domainmodel

import androidx.annotation.CheckResult
import com.krystianwsul.common.utils.TimeLogger
import com.mindorks.scheduler.Priority
import com.mindorks.scheduler.RxPS
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

private fun getScheduler(priority: Priority? = null) = RxPS.get(priority ?: Priority.MEDIUM)

private fun log(callSite: String) { // todo dependencies final cleanup
    TimeLogger.start("getScheduler").stop(callSite)
}

private fun getCallSite(): String {
    val stackTraceElements = Thread.currentThread().stackTrace
    val caller = stackTraceElements[3]

    return Exception().stackTraceToString()

    return "${caller.className}.${caller.methodName}"
}

private fun <T : Any> Observable<T>.logR(): Observable<T> {
    val callSite = getCallSite()

    return doOnNext { log(callSite) }
}

private fun <T : Any> Single<T>.logR(): Single<T> {
    val callSite = getCallSite()

    return doOnSuccess { log(callSite) }
}

private fun <T : Any> Flowable<T>.logR(): Flowable<T> {
    val callSite = getCallSite()

    return doOnNext { log(callSite) }
}

private fun Completable.logR(): Completable {
    val callSite = getCallSite()

    return doOnComplete { log(callSite) }
}

fun <T : Any> Observable<T>.observeOnDomain(priority: Priority? = null) =
    observeOn(getScheduler(priority))!!//.logR() todo dependencies final cleanup

fun <T : Any> Single<T>.observeOnDomain(priority: Priority? = null) =
    observeOn(getScheduler(priority))!!//.logR() todo dependencies final cleanup

fun <T : Any> Flowable<T>.observeOnDomain(priority: Priority? = null) =
    observeOn(getScheduler(priority))!!//.logR() todo dependencies final cleanup

fun <T : Any> Observable<T>.subscribeOnDomain(priority: Priority? = null) =
    subscribeOn(getScheduler(priority))!!//.logR() todo dependencies final cleanup

fun <T : Any> Single<T>.subscribeOnDomain(priority: Priority? = null) =
    subscribeOn(getScheduler(priority))!!//.logR() todo dependencies final cleanup

fun Completable.subscribeOnDomain(priority: Priority? = null) =
    subscribeOn(getScheduler(priority))!!//.logR() todo dependencies final cleanup

@CheckResult
fun completeOnDomain(action: () -> Unit) = Completable.fromAction(action).subscribeOnDomain()

fun runOnDomain(action: () -> Unit) = completeOnDomain(action).subscribe()!!