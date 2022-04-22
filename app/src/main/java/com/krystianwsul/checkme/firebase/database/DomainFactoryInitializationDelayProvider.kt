package com.krystianwsul.checkme.firebase.database

import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable

interface DomainFactoryInitializationDelayProvider {

    fun getDelayCompletable(rootTasksFactory: RootTasksFactory): Completable

    object Default : DomainFactoryInitializationDelayProvider {

        override fun getDelayCompletable(rootTasksFactory: RootTasksFactory) = Completable.complete()
    }

    class Task private constructor(private val taskKey: TaskKey.Root) : DomainFactoryInitializationDelayProvider {

        companion object {

            fun fromTaskKey(taskKey: TaskKey) = taskKey.let { it as? TaskKey.Root }?.let(::Task)
        }

        override fun getDelayCompletable(rootTasksFactory: RootTasksFactory): Completable {
            return rootTasksFactory.changeTypes
                .map { }
                .startWithItem(Unit)
                .filter { _ -> rootTasksFactory.tryGetRootTask(taskKey) != null }
                .firstOrError()
                .ignoreElement()
        }
    }
}