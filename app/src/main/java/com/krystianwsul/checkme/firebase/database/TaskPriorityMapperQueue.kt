package com.krystianwsul.checkme.firebase.database

object TaskPriorityMapperQueue {

    private val providers = LinkedHashSet<Provider>()

    fun addProvider(provider: Provider) {
        providers += provider
    }

    fun removeProvider(provider: Provider) {
        providers -= provider
    }

    fun tryGetCurrent() = providers.lastOrNull()?.newTaskPriorityMapper()

    interface Provider {

        fun newTaskPriorityMapper(): TaskPriorityMapper?
    }
}