package com.krystianwsul.checkme.firebase.loaders

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.junit.Assert

@ExperimentalStdlibApi
class EmissionChecker<T : Any>(
        val name: String,
        compositeDisposable: CompositeDisposable,
        source: Observable<T>
) {

    private val handlers = mutableListOf<(T) -> Unit>()

    constructor(
            name: String,
            compositeDisposable: CompositeDisposable,
            source: Single<T>
    ) : this(
            name,
            compositeDisposable,
            source.toObservable()
    )

    init {
        compositeDisposable += source.subscribe {
            try {
                handlers.first().invoke(it)
                handlers.removeFirst()
            } catch (exception: Exception) {
                throw EmissionException(name, it, exception)
            }
        }
    }

    fun addHandler(handler: (T) -> Unit) {
        handlers += handler
    }

    fun checkEmpty() = Assert.assertTrue("$name is not empty", handlers.isEmpty())

    private class EmissionException(
            name: String,
            value: Any,
            exception: Exception
    ) : Exception("name: $name, value: $value", exception)
}