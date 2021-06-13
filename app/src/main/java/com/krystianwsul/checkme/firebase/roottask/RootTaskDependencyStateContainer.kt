package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey

interface RootTaskDependencyStateContainer {

    fun onLoaded(rootTaskRecord: RootTaskRecord)

    fun onRemoved(taskKey: TaskKey.Root)

    fun hasDependentTasks(taskKey: TaskKey.Root): Boolean

    class Impl : RootTaskDependencyStateContainer {

        private val stateHoldersByTaskKey = mutableMapOf<TaskKey.Root, StateHolder>()

        private fun getStateHolder(taskKey: TaskKey.Root) = stateHoldersByTaskKey.getOrPut(taskKey) { StateHolder(taskKey) }

        override fun onLoaded(rootTaskRecord: RootTaskRecord) =
            getStateHolder(rootTaskRecord.taskKey).onLoaded(rootTaskRecord)

        override fun onRemoved(taskKey: TaskKey.Root) = getStateHolder(taskKey).onRemoved()

        override fun hasDependentTasks(taskKey: TaskKey.Root): Boolean {
            TODO("Not yet implemented")
        }

        private class StateHolder(val taskKey: TaskKey.Root) {

            private var loadState: LoadState = LoadState.Absent

            private val upStates = mutableMapOf<TaskKey.Root, RecordState>()

            fun onLoaded(rootTaskRecord: RootTaskRecord) {

            }

            fun onRemoved() {
            }

            fun addUpState(recordState: RecordState) {
                check(!upStates.containsKey(recordState.rootTaskRecord.taskKey))

                upStates[recordState.rootTaskRecord.taskKey] = recordState
            }

            fun removeUpState(recordState: RecordState) {
                check(upStates.remove(recordState.rootTaskRecord.taskKey) == recordState)
            }
        }

        private sealed class LoadState {

            object Absent : LoadState()

            class Loaded(initialRecordState: RecordState) : LoadState() {

                private var recordState = initialRecordState

                fun updateRecordState(newRecordState: RecordState) {
                    recordState.removeFromDownStateHolders()

                    recordState = newRecordState
                }
            }
        }

        private class RecordState(val rootTaskRecord: RootTaskRecord, impl: Impl) {

            val downTaskKeys = rootTaskRecord.getDependentTaskKeys()

            val downStateHolders = downTaskKeys.associateWith(impl::getStateHolder)

            init {
                downStateHolders.values.forEach { it.addUpState(this) }
            }

            fun removeFromDownStateHolders() = downStateHolders.values.forEach { it.removeUpState(this) }
        }
    }
}