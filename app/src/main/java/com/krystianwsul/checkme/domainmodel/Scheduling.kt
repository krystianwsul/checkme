package com.krystianwsul.checkme.domainmodel

import androidx.annotation.CheckResult
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

fun <T : Any> Observable<T>.observeOnDomain() = observeOn(Schedulers.single())!!
fun <T : Any> Single<T>.observeOnDomain() = observeOn(Schedulers.single())!!
fun <T : Any> Flowable<T>.observeOnDomain() = observeOn(Schedulers.single())!!

fun domainCompletable() = Completable.complete().subscribeOn(Schedulers.single())!! // todo scheduler

@CheckResult
fun completeOnDomain(action: () -> Unit) = Completable.fromAction(action).subscribeOn(Schedulers.single())!!

fun runOnDomain(action: () -> Unit) = completeOnDomain(action).subscribe()!!