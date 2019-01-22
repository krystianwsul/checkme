package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import org.junit.Assert
import org.junit.Test

class FactoryListenerTest {

    @Test
    fun test() {
        val userInfoRelay = PublishRelay.create<NullableWrapper<Unit>>()

        val taskSingleRelay = PublishRelay.create<Map<String, String>>()
        val friendSingleRelay = PublishRelay.create<Map<String, String>>()
        val userSingleRelay = PublishRelay.create<Map<String, String>>()

        val taskEventsRelay = PublishRelay.create<Pair<String, String>>()
        val friendObservableRelay = PublishRelay.create<Map<String, String>>()
        val userObservableRelay = PublishRelay.create<Map<String, String>>()

        val domainFactoryRelay = BehaviorRelay.createDefault(NullableWrapper<Output>())

        FactoryListener(
                userInfoRelay,
                { taskSingleRelay.firstOrError() },
                { friendSingleRelay.firstOrError() },
                { userSingleRelay.firstOrError() },
                { taskEventsRelay },
                { friendObservableRelay },
                { userObservableRelay },
                { userInfo, tasks, friends, user -> Output(tasks.toMutableMap(), friends, user) },
                {},
                { domainFactory, tasks -> domainFactory.tasks[tasks.first] = tasks.second },
                { domainFactory, friends -> domainFactory.friends = friends },
                { domainFactory, user -> domainFactory.user = user }
        ).domainFactoryObservable.subscribe(domainFactoryRelay)

        Assert.assertTrue(domainFactoryRelay.value!! == NullableWrapper<Output>())
    }

    data class Output(val tasks: MutableMap<String, String>, var friends: Map<String, String>, var user: Map<String, String>)
}