package com.krystianwsul.checkme.utils

import io.reactivex.rxjava3.core.Single

fun <T : Any, U : Any> Single<T>.mapWith(other: U) = map { it to other }!!

fun <T : Any> Single<T>.doOnSuccessOrDispose(action: () -> Unit) = doOnSuccess { action() }.doOnDispose { action() }!!
