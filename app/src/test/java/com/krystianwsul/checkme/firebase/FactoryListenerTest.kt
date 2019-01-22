package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.ReplayRelay
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import org.junit.Assert
import org.junit.Test

class FactoryListenerTest {

    @Test
    fun testInitial() {
        // setup

        val userInfoRelay = BehaviorRelay.create<NullableWrapper<Unit>>()

        val taskSingleRelay = PublishRelay.create<Map<String, String>>()
        val friendSingleRelay = PublishRelay.create<Map<String, String>>()
        val userSingleRelay = PublishRelay.create<Map<String, String>>()

        val taskEventsRelay = ReplayRelay.create<Pair<String, String>>()
        val friendObservableRelay = PublishRelay.create<Map<String, String>>()
        val userObservableRelay = PublishRelay.create<Map<String, String>>()

        val domainFactoryRelay = BehaviorRelay.createDefault(NullableWrapper<Output>())

        val task1 = "task key 1" to "task value a"
        val task2 = "task key 2" to "task value b"

        val initialTasks = mapOf(task1, task2)

        val task3 = "task key 3" to "task value c"

        val updatedTasks = mapOf(task1, task2, task3)

        // run

        Assert.assertTrue(domainFactoryRelay.value!! == NullableWrapper<Output>())

        userInfoRelay.accept(NullableWrapper(Unit))

        FactoryListener(
                userInfoRelay,
                { taskSingleRelay.firstOrError() },
                { friendSingleRelay.firstOrError() },
                { userSingleRelay.firstOrError() },
                {
                    taskEventsRelay.accept(task1)
                    taskEventsRelay.accept(task2)

                    taskEventsRelay
                },
                { friendObservableRelay },
                { userObservableRelay },
                { userInfo, tasks, friends, user ->
                    Output(tasks.toMutableMap(), friends, user)
                },
                {},
                { domainFactory, tasks ->
                    Assert.assertTrue(!domainFactory.tasks.contains(tasks.first))

                    domainFactory.tasks[tasks.first] = tasks.second
                },
                { domainFactory, friends -> domainFactory.friends = friends },
                { domainFactory, user -> domainFactory.user = user },
                { System.out.println(it) }
        ).domainFactoryObservable.subscribe(domainFactoryRelay)

        taskSingleRelay.accept(initialTasks)

        friendSingleRelay.accept(mapOf("friend key 1" to "friend value a"))

        userSingleRelay.accept(mapOf("user key 1" to "user value a"))

        Assert.assertTrue(domainFactoryRelay.value!!.value!!.tasks == initialTasks)

        taskEventsRelay.accept(task3)

        Assert.assertTrue(domainFactoryRelay.value!!.value!!.tasks == updatedTasks)
    }

    data class Output(val tasks: MutableMap<String, String>, var friends: Map<String, String>, var user: Map<String, String>)
}