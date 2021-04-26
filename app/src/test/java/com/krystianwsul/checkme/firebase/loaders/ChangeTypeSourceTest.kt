package com.krystianwsul.checkme.firebase.loaders

import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Test

class ChangeTypeSourceTest {

    private val domainDisposable = CompositeDisposable()

    @After
    fun after() {
        domainDisposable.clear()
    }

    @Test
    fun testInitial() {
        val changeTypeSource = ChangeTypeSource(
                Single.never(),
                Single.never(),
                DatabaseRx(domainDisposable, Observable.never()),
                Single.never(),
                mockk(),
        )

        val emissionChecker = EmissionChecker("changeTypes", domainDisposable, changeTypeSource.changeTypes)

        emissionChecker.checkEmpty()
    }
}