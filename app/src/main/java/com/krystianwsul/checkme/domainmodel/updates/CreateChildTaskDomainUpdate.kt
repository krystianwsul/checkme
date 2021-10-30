package com.krystianwsul.checkme.domainmodel.updates

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.update.AbstractSingleDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.newUuid
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class CreateChildTaskDomainUpdate(
    private val notificationType: DomainListenerManager.NotificationType,
    private val parent: Parent,
    private val createParameters: EditDelegate.CreateParameters,
    private val copyTaskKey: TaskKey? = null,
) : AbstractSingleDomainUpdate<EditDelegate.CreateResult>("createChildTask") {

    override fun doAction(
        domainFactory: DomainFactory,
        now: ExactTimeStamp.Local
    ): DomainUpdater.Result<EditDelegate.CreateResult> {
        val imageUuid = createParameters.imagePath?.let { newUuid() } // todo add instance new obj
        val imageJson = imageUuid?.let { TaskJson.Image(it, domainFactory.uuid) }

        val childTask = domainFactory.trackRootTaskIds {
            when (parent) {
                is Parent.Task -> {
                    val parentTask = domainFactory.convertToRoot(domainFactory.getTaskForce(parent.taskKey), now)
                    parentTask.requireNotDeleted()

                    domainFactory.createChildTask(
                        now,
                        parentTask,
                        createParameters.name,
                        createParameters.note,
                        imageJson,
                        copyTaskKey,
                    )
                }
                is Parent.Instance -> {
                    val parentTask = domainFactory.convertToRoot(
                        domainFactory.getTaskForce(parent.instanceKey.taskKey),
                        now,
                    )

                    parentTask.requireNotDeleted()

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
                        imageUuid,
                        domainFactory,
                    ).also { it.getInstance(migratedInstanceScheduleKey).setParentState(parentInstance.instanceKey) }
                }
            }
        }

        imageUuid?.let {
            Uploader.addUpload(domainFactory.deviceDbInfo, childTask.taskKey, it, createParameters.imagePath!!)
        }

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