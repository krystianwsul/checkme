package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.models.cache.InvalidatableCache
import com.krystianwsul.common.firebase.models.cache.invalidatableCache

class RootTaskDependencyResolver(private val rootTask: RootTask) {

    val dependenciesLoadedCache: InvalidatableCache<Boolean> =
        invalidatableCache(rootTask.clearableInvalidatableManager) { invalidatableCache ->
            val taskRecord = rootTask.taskRecord
            val userCustomTimeProvider = rootTask.userCustomTimeProvider
            val rootModelChangeManager = rootTask.rootModelChangeManager
            val parent = rootTask.parent

            val customTimeKeys = taskRecord.getUserCustomTimeKeys()
            val customTimes = customTimeKeys.mapNotNull(userCustomTimeProvider::tryGetUserCustomTime)

            if (customTimes.size < customTimeKeys.size) {
                val removable = rootModelChangeManager.userInvalidatableManager.addInvalidatable(invalidatableCache)

                return@invalidatableCache InvalidatableCache.ValueHolder(false) { removable.remove() }
            }

            val taskKeys = taskRecord.getDirectDependencyTaskKeys()
            val tasks = taskKeys.mapNotNull(parent::tryGetRootTask)

            if (tasks.size < taskKeys.size) {
                val removable = rootModelChangeManager.rootTaskInvalidatableManager.addInvalidatable(invalidatableCache)

                return@invalidatableCache InvalidatableCache.ValueHolder(false) { removable.remove() }
            }

            val customTimeRemovables = customTimes.map {
                it.user
                    .clearableInvalidatableManager
                    .addInvalidatable(invalidatableCache)
            }

            val taskRemovables = tasks.map {
                it.rootTaskDependencyResolver
                    .dependenciesLoadedCache
                    .invalidatableManager
                    .addInvalidatable(invalidatableCache)
            }

            InvalidatableCache.ValueHolder(tasks.all { it.dependenciesLoaded }) {
                taskRemovables.forEach { it.remove() }
                customTimeRemovables.forEach { it.remove() }
            }
        }
}