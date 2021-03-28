package com.krystianwsul.checkme.domainmodel.update

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.mockk.mockk
import org.junit.Test

class AndroidDomainUpdaterTest {

    @Test
    fun testQueue() {
        val isReady = PublishRelay.create<NullableWrapper<DomainFactory>>()

        val queue = AndroidDomainUpdater.Queue(isReady)
        val disposable = queue.subscribe()

        val results = mutableListOf<Int>()

        isReady.accept(NullableWrapper(mockk()))

        disposable.dispose()
    }
}