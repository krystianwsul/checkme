package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.merge

interface RootTaskToRootTaskCoordinator {

    fun getRootTasks(rootTaskRecord: RootTaskRecord): Completable

    class Impl(private val rootTaskKeySource: RootTaskKeySource) : RootTaskToRootTaskCoordinator {

        override fun getRootTasks(rootTaskRecord: RootTaskRecord): Completable {
            val allRelatedTasks: Set<TaskKey.Root> = rootTaskRecord.rootTaskParentDelegate.rootTaskKeys +
                    rootTaskRecord.taskHierarchyRecords.map { TaskKey.Root(it.value.parentTaskId) }

            rootTaskKeySource.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, allRelatedTasks)

            /**
             * todo task fetch return after tasks are loaded.
             *
             * Note: this needs to block not only until the immediate children are loaded, but until the entire graph
             * is complete.  Yeah, figuring that out is gonna be fun.
             */

            /**
             * Ho hum.  We need a way to determine which task records have been loaded.  Ultimately, this delay is so
             * that by the time a project is initialized, all the tasks, and all the custom times they use, are all
             * initialized.  That part's something we can check at the TaskFactory level, like how we check for custom
             * times.  (Since the task model objects should be ready before the project model is constructed.)
             *
             * But it's trickier for individual tasks.  Here, we need to wait for all child task records to be ready,
             * and all of their custom times to be ready.
             */

            return Completable.complete()
        }

        class TreeLoadState(val taskLoadStates: Map<TaskKey.Root, TaskLoadState>) {

            fun getNextState(): Maybe<TreeLoadState> {
                val mutatorSingles = taskLoadStates.mapNotNull { it.value.mutator }

                return if (mutatorSingles.isEmpty()) {
                    Maybe.empty()
                } else {
                    val nextMutatorSingle = mutatorSingles.map { it.toObservable() }
                            .merge()
                            .firstOrError()

                    nextMutatorSingle.map { TreeLoadState(it.mutateMap(taskLoadStates)) }.toMaybe()
                }
            }
        }

        sealed class TaskLoadState {

            abstract val mutator: Single<TaskLoadStateMapMutator>?

            class LoadingRecord(
                    val taskKey: TaskKey.Root,
                    val taskRecordLoader: TaskRecordLoader,
                    val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
            ) : TaskLoadState() {

                override val mutator = taskRecordLoader.getTaskRecordSingle(taskKey)
                        .map<TaskLoadStateMapMutator> {
                            RecordLoadedMutator(it, taskRecordLoader, rootTaskUserCustomTimeProviderSource)
                        }
                        .cache()!!
            }

            class LoadingTimes(
                    val taskRecord: RootTaskRecord,
                    val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
            ) : TaskLoadState() {

                override val mutator = rootTaskUserCustomTimeProviderSource.getUserCustomTimeProvider(taskRecord)
                        .map<TaskLoadStateMapMutator> { TimesLoadedMutator(taskRecord) }
                        .cache()!!
            }

            class Loaded(val taskRecord: RootTaskRecord) : TaskLoadState() {

                override val mutator: Single<TaskLoadStateMapMutator>? = null
            }
        }

        interface TaskLoadStateMapMutator {

            fun mutateMap(oldMap: Map<TaskKey.Root, TaskLoadState>): Map<TaskKey.Root, TaskLoadState>
        }

        class RecordLoadedMutator(
                val taskRecord: RootTaskRecord,
                val taskRecordLoader: TaskRecordLoader,
                val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
        ) : TaskLoadStateMapMutator {

            override fun mutateMap(oldMap: Map<TaskKey.Root, TaskLoadState>): Map<TaskKey.Root, TaskLoadState> {
                val parentKeys = taskRecord.taskHierarchyRecords.map { TaskKey.Root(it.value.parentTaskId) }
                val childKeys = taskRecord.rootTaskParentDelegate.rootTaskKeys
                val allKeys = (parentKeys + childKeys).toSet()

                val newKeys = allKeys - oldMap.keys

                val newEntries = newKeys.map {
                    it to TaskLoadState.LoadingRecord(it, taskRecordLoader, rootTaskUserCustomTimeProviderSource)
                }

                val newMap = (oldMap + newEntries).toMutableMap()
                check(newMap.getValue(taskRecord.taskKey) is TaskLoadState.LoadingRecord)

                newMap[taskRecord.taskKey] =
                        TaskLoadState.LoadingTimes(taskRecord, rootTaskUserCustomTimeProviderSource)

                return oldMap + newEntries
            }
        }

        class TimesLoadedMutator(val taskRecord: RootTaskRecord) : TaskLoadStateMapMutator {

            override fun mutateMap(oldMap: Map<TaskKey.Root, TaskLoadState>): Map<TaskKey.Root, TaskLoadState> {
                check(oldMap.containsKey(taskRecord.taskKey))

                return oldMap.toMutableMap().also {
                    it[taskRecord.taskKey] = TaskLoadState.Loaded(taskRecord)
                }
            }
        }

        interface TaskRecordLoader {

            fun getTaskRecordSingle(taskKey: TaskKey.Root): Single<RootTaskRecord>
        }
    }
}