package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
import com.krystianwsul.checkme.gui.instances.drag.DropParent
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.ShowInstanceViewModel
import com.krystianwsul.common.domain.TaskUndoData
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getShowInstanceData(
    instanceKey: InstanceKey,
    now: ExactTimeStamp.Local = ExactTimeStamp.Local.now,
): ShowInstanceViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowInstanceData")

    DomainThreadChecker.instance.requireDomainThread()

    val task = getTaskForce(instanceKey.taskKey)

    val instance = getInstance(instanceKey)
    val instanceDateTime = instance.instanceDateTime
    val parentInstance = instance.parentInstance

    var displayText = listOfNotNull(
        instance.parentInstance?.name,
        instanceDateTime.takeIf { instance.isRootInstance() }?.getDisplayText(),
    ).joinToString("\n\n")

    if (debugMode) {
        displayText += "\n\ntask key: " + instanceKey.taskKey
        displayText += "\ndate: " + instanceKey.instanceScheduleKey.scheduleDate
        displayText += "\ncustom time: " + instanceKey.instanceScheduleKey.scheduleTimePair.customTimeKey
        displayText += "\nnormal time: " + instanceKey.instanceScheduleKey.scheduleTimePair.hourMinute
        displayText += "\nexists? " + instance.exists()
        displayText += "\nisVisible? " + instance.isVisibleDebug(
            now,
            Instance.VisibilityOptions(hack24 = true, assumeRoot = true)
        ).let { "${it.first}, ${it.second}" }
    }

    return ShowInstanceViewModel.Data(
        instance.name,
        instanceDateTime,
        instance.done != null,
        task.notDeleted,
        instance.canMigrateDescription(now),
        parentInstance == null,
        getGroupListData(instance, task, now),
        displayText,
        task.taskKey,
        debugMode || instance.isVisible(now, Instance.VisibilityOptions(hack24 = true)),
        instanceKey,
    )
}

@CheckResult
fun DomainUpdater.setTaskEndTimeStamps(
    notificationType: DomainListenerManager.NotificationType,
    taskKeys: Set<TaskKey>,
    deleteInstances: Boolean,
    instanceKey: InstanceKey,
): Single<Pair<TaskUndoData, Boolean>> = SingleDomainUpdate.create("setTaskEndTimeStamps") { now ->
    val (taskUndoData, params) = setTaskEndTimeStamps(notificationType, taskKeys, deleteInstances, now)

    DomainUpdater.Result(
        Pair(
            taskUndoData,
            debugMode || getInstance(instanceKey).isVisible(now, Instance.VisibilityOptions(hack24 = true)),
        ),
        params,
    )
}.perform(this)

@CheckResult
fun DomainUpdater.splitInstance(
    notificationType: DomainListenerManager.NotificationType,
    instanceKey: InstanceKey,
): Completable = CompletableDomainUpdate.create("splitInstance") { now ->
    val instance = getInstance(instanceKey)

    val childInstances = instance.getChildInstances()
    check(childInstances.size > 1)

    trackRootTaskIds {
        if (instance.parentInstance != null) {
            childInstances.forEach { it.setParentState(instance.parentInstance!!.instanceKey) }
        } else {
            childInstances.forEach {
                it.setParentState(Instance.ParentState.NoParent)

                val instanceDateTime = instance.instanceDateTime

                it.setInstanceDateTime(shownFactory, instanceDateTime, this, now)
            }
        }
    }

    instance.hide()

    val remoteProjects = (childInstances + instance).map { it.getProject() }

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(remoteProjects))
}.perform(this)

private fun DomainFactory.getGroupListData(
    parentInstance: Instance,
    task: Task,
    now: ExactTimeStamp.Local,
): GroupListDataWrapper {
    val customTimeDatas = getCurrentRemoteCustomTimes().map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val instanceDescriptors = parentInstance.getChildInstances()
        .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true)) }
        .map { childInstance ->
            val childTask = childInstance.task

            val (notDoneChildInstanceDescriptors, doneChildInstanceDescriptors) = getChildInstanceDatas(childInstance, now)

            val instanceData = GroupListDataWrapper.InstanceData(
                childInstance.done,
                childInstance.instanceKey,
                childInstance.name,
                childInstance.instanceDateTime.timeStamp,
                childInstance.instanceDate,
                childTask.notDeleted,
                childTask.isVisible(now),
                childInstance.canMigrateDescription(now),
                childInstance.getCreateTaskTimePair(projectsFactory.privateProject, myUserFactory.user),
                childTask.note,
                newMixedInstanceDataCollection(notDoneChildInstanceDescriptors),
                doneChildInstanceDescriptors.toDoneSingleBridges(),
                childInstance.ordinal,
                childTask.getImage(deviceDbInfo),
                childInstance.isAssignedToMe(now, myUserFactory.user),
                childInstance.getProjectInfo(),
                childInstance.getProject().projectKey as? ProjectKey.Shared,
                childInstance.parentInstance?.instanceKey,
            )

            GroupTypeFactory.InstanceDescriptor(
                instanceData,
                childInstance.instanceDateTime.toDateTimePair(),
                childInstance.groupByProject,
                childInstance,
            )
        }

    val (mixedInstanceDescriptors, doneInstanceDescriptors) = instanceDescriptors.splitDone()

    return GroupListDataWrapper(
        customTimeDatas,
        parentInstance.canAddSubtask(now),
        listOf(),
        task.note,
        newMixedInstanceDataCollection(mixedInstanceDescriptors),
        doneInstanceDescriptors.toDoneSingleBridges(),
        task.getImage(deviceDbInfo),
        parentInstance.getProjectInfo(),
        DropParent.ParentInstance(parentInstance.instanceKey),
    )
}