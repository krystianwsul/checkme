package com.krystianwsul.checkme.firebase.database

import com.jakewharton.rxrelay3.BehaviorRelay

object TaskPriorityMapperQueue {

    private val providers = LinkedHashSet<Provider>()

    private val trigger = BehaviorRelay.create<Unit>()

    val delayObservable
        get() = trigger.map {
            providers.lastOrNull()
                ?.newDelayProvider()
                ?: DomainFactoryInitializationDelayProvider.Default
        }

    fun addProvider(provider: Provider) {
        providers += provider

        trigger.accept(Unit)
    }

    fun removeProvider(provider: Provider) {
        providers -= provider

        trigger.accept(Unit)
    }

    fun tryGetCurrent() = providers.lastOrNull()?.newTaskPriorityMapper()

    interface Provider {

        fun newDelayProvider(): DomainFactoryInitializationDelayProvider?

        fun newTaskPriorityMapper(): TaskPriorityMapper?
    }
}