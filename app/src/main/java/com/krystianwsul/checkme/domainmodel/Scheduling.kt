package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.firebase.SchedulerType
import com.krystianwsul.common.firebase.SchedulerTypeHolder
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

fun <T : Any> Observable<T>.observeOnDomain() = observeOn(Schedulers.single()).doOnNext { setSchedulerType() }!!

fun <T : Any> Single<T>.observeOnDomain() = observeOn(Schedulers.single()).doOnSuccess { setSchedulerType() }!!

fun <T : Any> Flowable<T>.observeOnDomain() = observeOn(Schedulers.single()).doOnNext { setSchedulerType() }!!

private fun setSchedulerType() = SchedulerTypeHolder.instance.set(SchedulerType.DOMAIN)