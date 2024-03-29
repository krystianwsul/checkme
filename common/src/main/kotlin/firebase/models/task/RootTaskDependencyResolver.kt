package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.models.cache.Invalidatable
import com.krystianwsul.common.firebase.models.cache.InvalidatableCache
import com.krystianwsul.common.firebase.models.cache.invalidatableCache
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.utils.TaskKey

class RootTaskDependencyResolver(private val rootTask: RootTask) : Invalidatable {

    private sealed class DirectDependencyState {

        object Absent : DirectDependencyState()

        class Present(val tasks: List<RootTask>) : DirectDependencyState()
    }

    private val directDependenciesStateCache: InvalidatableCache<DirectDependencyState> =
        invalidatableCache(rootTask.clearableInvalidatableManager) { invalidatableCache ->
            val taskRecord = rootTask.taskRecord
            val userCustomTimeProvider = rootTask.userCustomTimeProvider
            val rootModelChangeManager = rootTask.rootModelChangeManager
            val parent = rootTask.parent

            var project: Project<*>? = null

            rootTask.directDependencyProjectKey?.let {
                project = rootTask.parent.getProjectIfPresent(it)

                if (project == null) {
                    val projectsRemovable =
                        rootModelChangeManager.foreignProjectLoadedInvalidatableManager.addInvalidatable(
                            invalidatableCache
                        )

                    return@invalidatableCache InvalidatableCache.ValueHolder(DirectDependencyState.Absent) {
                        projectsRemovable.remove()
                    }
                }
            }

            val customTimeKeys = taskRecord.getUserCustomTimeKeys()
            val customTimes = customTimeKeys.mapNotNull(userCustomTimeProvider::tryGetUserCustomTime)

            if (customTimes.size < customTimeKeys.size) {
                val removable = rootModelChangeManager.userInvalidatableManager.addInvalidatable(invalidatableCache)

                return@invalidatableCache InvalidatableCache.ValueHolder(DirectDependencyState.Absent) { removable.remove() }
            }

            val taskKeys = taskRecord.getDirectDependencyTaskKeys()
            val tasks = taskKeys.mapNotNull(parent::tryGetRootTask)

            if (tasks.size < taskKeys.size) {
                val removable = rootModelChangeManager.rootTaskInvalidatableManager.addInvalidatable(invalidatableCache)

                return@invalidatableCache InvalidatableCache.ValueHolder(DirectDependencyState.Absent) { removable.remove() }
            }

            val projectRemovable = project?.clearableInvalidatableManager?.addInvalidatable(invalidatableCache)

            val customTimeRemovables = customTimes.map {
                it.user
                    .clearableInvalidatableManager
                    .addInvalidatable(invalidatableCache)
            }

            val taskRemovables = tasks.map { it.clearableInvalidatableManager.addInvalidatable(invalidatableCache) }

            InvalidatableCache.ValueHolder(DirectDependencyState.Present(tasks)) {
                projectRemovable?.remove()
                customTimeRemovables.forEach { it.remove() }
                taskRemovables.forEach { it.remove() }
            }
        }

    val dependenciesLoadedCache: InvalidatableCache<Boolean> =
        invalidatableCache(rootTask.clearableInvalidatableManager) { invalidatableCache ->
            val directDependencyStateRemovable =
                directDependenciesStateCache.invalidatableManager.addInvalidatable(invalidatableCache)

            when (val directDependenciesState = directDependenciesStateCache.value) {
                DirectDependencyState.Absent ->
                    InvalidatableCache.ValueHolder(false) { directDependencyStateRemovable.remove() }
                is DirectDependencyState.Present -> {
                    val tasks = directDependenciesState.tasks

                    val taskRemovables = tasks.map {
                        it.rootTaskDependencyResolver
                            .dependenciesLoadedCache
                            .invalidatableManager
                            .addInvalidatable(invalidatableCache)
                    }

                    val checkedTaskKeys = mutableSetOf<TaskKey.Root>()

                    InvalidatableCache.ValueHolder(tasks.all { checkDirectDependenciesRecursive(it, checkedTaskKeys) }) {
                        directDependencyStateRemovable.remove()

                        taskRemovables.forEach { it.remove() }
                    }
                }
            }
        }

    val dependenciesLoaded get() = dependenciesLoadedCache.value

    val directDependencyTasks
        get() = directDependenciesStateCache.value
            .let { it as DirectDependencyState.Present }
            .tasks

    override fun invalidate() = directDependenciesStateCache.invalidate()

    private fun checkDirectDependenciesRecursive(task: RootTask, checkedTaskKeys: MutableSet<TaskKey.Root>): Boolean {
        if (task.taskKey in checkedTaskKeys) return true

        checkedTaskKeys += task.taskKey

        task.rootTaskDependencyResolver
            .dependenciesLoadedCache
            .takeIf { it.isInitialized() }
            ?.let { return it.value }

        return when (val directDependenciesState = task.rootTaskDependencyResolver.directDependenciesStateCache.value) {
            DirectDependencyState.Absent -> false
            is DirectDependencyState.Present ->
                directDependenciesState.tasks.all { checkDirectDependenciesRecursive(it, checkedTaskKeys) }
        }
    }
}