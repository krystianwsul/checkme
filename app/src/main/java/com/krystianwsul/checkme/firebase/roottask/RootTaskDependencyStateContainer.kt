package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey

interface RootTaskDependencyStateContainer {

    fun onLoaded(rootTaskRecord: RootTaskRecord)

    fun onRemoved(taskKey: TaskKey.Root)

    fun hasDependentTasks(taskKey: TaskKey.Root): Boolean

    class Impl : RootTaskDependencyStateContainer {

        private val stateHoldersByTaskKey = mutableMapOf<TaskKey.Root, StateHolder>()

        private fun getStateHolder(taskKey: TaskKey.Root) =
            stateHoldersByTaskKey.getOrPut(taskKey) { StateHolder(taskKey, this) }

        override fun onLoaded(rootTaskRecord: RootTaskRecord) =
            getStateHolder(rootTaskRecord.taskKey).onLoaded(rootTaskRecord)

        override fun onRemoved(taskKey: TaskKey.Root) = getStateHolder(taskKey).onRemoved()

        override fun hasDependentTasks(taskKey: TaskKey.Root): Boolean {
            return getStateHolder(taskKey).hasAllDependencies
        }

        private class StateHolder(val taskKey: TaskKey.Root, private val impl: Impl) {

            private var loadState: LoadState = LoadState.Absent

            private val upStates = mutableMapOf<TaskKey.Root, RecordState>()

            val hasAllDependencies get() = loadState.hasAllDependencies

            fun onLoaded(rootTaskRecord: RootTaskRecord) {
                val newRecordState = RecordState(rootTaskRecord, impl, this)

                when (val oldLoadState = loadState) {
                    LoadState.Absent -> {
                        loadState = LoadState.Loaded(newRecordState)
                    }
                    is LoadState.Loaded -> {
                        oldLoadState.updateRecordState(newRecordState)
                    }
                }
            }

            fun onRemoved() {
                val oldLoadState = loadState

                if (oldLoadState is LoadState.Loaded) {
                    if (oldLoadState.hasAllDependencies) propagateClearHasAllDependencies()

                    oldLoadState.recordState.removeFromDownStateHolders()
                }

                loadState = LoadState.Absent
            }

            fun addUpState(recordState: RecordState) {
                check(!upStates.containsKey(recordState.rootTaskRecord.taskKey))

                upStates[recordState.rootTaskRecord.taskKey] = recordState
            }

            fun removeUpState(recordState: RecordState) {
                check(upStates.remove(recordState.rootTaskRecord.taskKey) == recordState)
            }

            fun propagateClearHasAllDependencies() {
                upStates.values.forEach { it.clearHasAllDependencies() }
            }
        }

        private sealed class LoadState {

            abstract val hasAllDependencies: Boolean

            object Absent : LoadState() {

                override val hasAllDependencies = false
            }

            class Loaded(initialRecordState: RecordState) : LoadState() {

                var recordState = initialRecordState
                    private set

                init {
                    recordState.addToDownStateHolders()
                }

                override val hasAllDependencies get() = recordState.hasAllDependencies

                fun updateRecordState(newRecordState: RecordState) {
                    recordState.removeFromDownStateHolders()

                    recordState = newRecordState

                    recordState.addToDownStateHolders()
                }
            }
        }

        private class RecordState(val rootTaskRecord: RootTaskRecord, impl: Impl, private val stateHolder: StateHolder) {

            val downTaskKeys = rootTaskRecord.getDependentTaskKeys()

            val downStateHolders = downTaskKeys.associateWith(impl::getStateHolder)

            var hasAllDependencies = false
                private set

            fun updateHasAllDependencies() {
                hasAllDependencies = downStateHolders.values.all { it.hasAllDependencies }
            }

            init {
                updateHasAllDependencies()
            }

            fun clearHasAllDependencies() {
                if (hasAllDependencies) {
                    hasAllDependencies = false

                    stateHolder.propagateClearHasAllDependencies()
                }
            }

            fun addToDownStateHolders() = downStateHolders.values.forEach { it.addUpState(this) }
            fun removeFromDownStateHolders() = downStateHolders.values.forEach { it.removeUpState(this) }
        }
    }
}