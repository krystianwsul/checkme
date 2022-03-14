package com.krystianwsul.checkme.domainmodel.updates

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.update.AbstractSingleDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class CreateChildTaskDomainUpdate(
    private val notificationType: DomainListenerManager.NotificationType,
    private val parent: Parent,
    private val createParameters: EditDelegate.CreateParameters,
    private val copySource: EditParameters.Copy.CopySource? = null,
    private val clearParentNote: Boolean = false,
) : AbstractSingleDomainUpdate<EditDelegate.CreateResult>("createChildTask") {

    override fun doAction(
        domainFactory: DomainFactory,
        now: ExactTimeStamp.Local,
    ): DomainUpdater.Result<EditDelegate.CreateResult> {
        val image = createParameters.getImage(domainFactory)

        val childTask = domainFactory.trackRootTaskIds {
            when (parent) {
                is Parent.Task -> {
                    val parentTask = domainFactory.convertToRoot(domainFactory.getTaskForce(parent.taskKey), now)
                    parentTask.requireNotDeleted()

                    if (clearParentNote) parentTask.clearNote()

                    domainFactory.createChildTask(
                        now,
                        parentTask,
                        createParameters.name,
                        createParameters.note,
                        image?.json,
                        copySource,
                    )
                }
                is Parent.Instance -> {
                    val parentTask = domainFactory.convertToRoot(
                        domainFactory.getTaskForce(parent.instanceKey.taskKey),
                        now,
                    )

                    parentTask.requireNotDeleted()

                    if (clearParentNote) parentTask.clearNote()

                    val migratedInstanceScheduleKey = domainFactory.migrateInstanceScheduleKey(
                        parentTask,
                        parent.instanceKey.instanceScheduleKey,
                        now,
                    )

                    val parentInstance = parentTask.getInstance(migratedInstanceScheduleKey)

                    val scheduleTime = parentInstance.scheduleTime

                    domainFactory.createScheduleTopLevelTask(
                        now,
                        createParameters.name,
                        listOf(Pair(ScheduleData.Single(parentInstance.scheduleDate, scheduleTime.timePair), scheduleTime)),
                        createParameters.note,
                        parentTask.project.projectKey,
                        image,
                        domainFactory,
                    ).also { it.getInstance(migratedInstanceScheduleKey).setParentState(parentInstance.instanceKey) }
                }
            }
        }

        image?.upload(childTask.taskKey)

        return DomainUpdater.Result(
            childTask.toCreateResult(now),
            true,
            notificationType,
            DomainFactory.CloudParams(childTask.project),
        )
    }

    sealed interface Parent {

        data class Task(val taskKey: TaskKey) : Parent
        data class Instance(val instanceKey: InstanceKey) : Parent
    }
}