package com.krystianwsul.checkme.firebase.loaders

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class EmissionChecker<T : Any>(
        val name: String,
        compositeDisposable: CompositeDisposable,
        source: Observable<T>
) {

    private val handlers = mutableListOf<(T) -> Unit>()

    constructor(name: String, compositeDisposable: CompositeDisposable, source: Single<T>) :
            this(name, compositeDisposable, source.toObservable())

    private var hasErrors = false

    init {
        compositeDisposable += source.subscribe {
            try {
                handlers.first().invoke(it)
                handlers.removeFirst()
            } catch (exception: Exception) {
                hasErrors = true
                throw EmissionException(name, it, exception)
            }
        }
    }

    fun checkNoErrors() = assertFalse(hasErrors)

    fun checkNoErrors(action: () -> Unit) {
        checkNoErrors()

        action()

        checkNoErrors()
    }

    fun addHandler(handler: (T) -> Unit) {
        check(!hasErrors)

        handlers += handler
    }

    fun checkEmpty() = assertTrue(
            "$name is not empty (as in, event not emitted as expected)",
            handlers.isEmpty(),
    )

    private class EmissionException(
            name: String,
            value: Any,
            exception: Exception,
    ) : Exception("name: $name, value: $value", exception)
}