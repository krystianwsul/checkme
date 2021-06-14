package com.krystianwsul.checkme.firebase.roottask

import androidx.annotation.VisibleForTesting
import com.krystianwsul.common.utils.TaskKey

interface RootTaskDependencyStateContainer {

    fun onLoaded(taskBridge: TaskBridge)

    fun onRemoved(taskKey: TaskKey.Root)

    fun isComplete(taskKey: TaskKey.Root): Boolean

    interface TaskBridge { // todo load revert to record

        val taskKey: TaskKey.Root

        val downKeys: Set<TaskKey.Root>
    }

    class Impl : RootTaskDependencyStateContainer {

        @VisibleForTesting
        val stateHoldersByTaskKey = mutableMapOf<TaskKey.Root, StateHolder>()

        private fun getStateHolder(taskKey: TaskKey.Root) =
            stateHoldersByTaskKey.getOrPut(taskKey) { StateHolder(taskKey, this) }

        override fun onLoaded(taskBridge: TaskBridge) =
            getStateHolder(taskBridge.taskKey).onLoaded(taskBridge)

        override fun onRemoved(taskKey: TaskKey.Root) = getStateHolder(taskKey).onRemoved()

        override fun isComplete(taskKey: TaskKey.Root): Boolean {
            val checkedKeys = mutableSetOf<TaskKey.Root>()
            val recordStatesToUpdate = mutableSetOf<RecordState>()

            val result = tryAccumulateKeys(taskKey, checkedKeys, recordStatesToUpdate)

            recordStatesToUpdate.forEach {
                check(it.isComplete == null)

                it.isComplete = result
            }

            return result
        }

        private fun tryAccumulateKeys(
            taskKey: TaskKey.Root,
            checkedKeys: MutableSet<TaskKey.Root>,
            recordStatesToUpdate: MutableSet<RecordState>,
        ): Boolean {
            checkedKeys += taskKey

            val stateHolder = getStateHolder(taskKey)

            val recordState = stateHolder.getRecordState() ?: return false

            recordState.isComplete?.let { return it }

            recordStatesToUpdate += recordState

            val downKeys = recordState.downKeys

            val newKeys = downKeys - checkedKeys

            return newKeys.all { tryAccumulateKeys(it, checkedKeys, recordStatesToUpdate) }
        }

        @VisibleForTesting
        class StateHolder(val taskKey: TaskKey.Root, private val impl: Impl) {

            private var loadState: LoadState = LoadState.Absent

            private val upStates = mutableMapOf<TaskKey.Root, RecordState>()

            val isComplete get() = loadState.isComplete

            fun onLoaded(taskBridge: TaskBridge) {
                val previousIsComplete = isComplete
                val oldLoadState = loadState

                val newRecordState = RecordState(taskBridge, impl, this)

                when (oldLoadState) {
                    LoadState.Absent -> {
                        val loaded = LoadState.Loaded(newRecordState)
                        loadState = loaded
                    }
                    is LoadState.Loaded -> {
                        oldLoadState.updateRecordState(newRecordState)
                    }
                }

                check(isComplete != false)

                if (previousIsComplete != null) invalidateUpStates()
            }

            fun onRemoved() {
                val previousIsComplete = isComplete
                val oldLoadState = loadState

                if (oldLoadState is LoadState.Loaded) {
                    oldLoadState.recordState.removeFromDownStateHolders()
                }

                loadState = LoadState.Absent

                check(isComplete == false)

                if (previousIsComplete == true) invalidateUpStates()
            }

            fun addUpState(recordState: RecordState) {
                check(!upStates.containsKey(recordState.taskBridge.taskKey))

                upStates[recordState.taskBridge.taskKey] = recordState
            }

            fun removeUpState(recordState: RecordState) {
                check(upStates.remove(recordState.taskBridge.taskKey) == recordState)
            }

            fun getRecordState() = loadState.recordState

            private fun invalidateUpStates() {
                upStates.values.forEach { it.propagateInvalidation() }
            }

            fun propagateInvalidation() {
                if (isComplete == null) return

                loadState.invalidateIsComplete()

                invalidateUpStates()
            }
        }

        private sealed class LoadState {

            abstract val isComplete: Boolean?

            abstract val recordState: RecordState?

            abstract fun invalidateIsComplete()

            object Absent : LoadState() {

                override val isComplete = false

                override val recordState: RecordState? = null

                override fun invalidateIsComplete() {}
            }

            class Loaded(initialRecordState: RecordState) : LoadState() {

                override var recordState = initialRecordState
                    private set

                override val isComplete get() = recordState.isComplete

                init {
                    recordState.addToDownStateHolders()
                }

                fun updateRecordState(newRecordState: RecordState) {
                    recordState.removeFromDownStateHolders()

                    recordState = newRecordState

                    recordState.addToDownStateHolders()
                }

                override fun invalidateIsComplete() {
                    recordState.isComplete = null
                }
            }
        }

        @VisibleForTesting
        class RecordState(val taskBridge: TaskBridge, impl: Impl, private val stateHolder: StateHolder) {

            val downKeys = taskBridge.downKeys

            private val downStateHolders = downKeys.associateWith(impl::getStateHolder)

            var isComplete: Boolean? = if (downStateHolders.isEmpty()) true else null
                set(value) {
                    if (downStateHolders.isEmpty()) {
                        check(field == true)
                    } else {
                        field = value
                    }
                }

            fun addToDownStateHolders() = downStateHolders.values.forEach { it.addUpState(this) }
            fun removeFromDownStateHolders() = downStateHolders.values.forEach { it.removeUpState(this) }

            fun propagateInvalidation() = stateHolder.propagateInvalidation()
        }
    }
}