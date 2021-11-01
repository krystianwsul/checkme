package com.krystianwsul.checkme.firebase.loaders

import io.reactivex.rxjava3.plugins.RxJavaPlugins
import org.junit.Assert

class RxErrorChecker {

    private val errors = mutableListOf<Throwable>()

    init {
        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            errors.add(it)
        }
    }

    fun check() = Assert.assertTrue(errors.isEmpty())
}