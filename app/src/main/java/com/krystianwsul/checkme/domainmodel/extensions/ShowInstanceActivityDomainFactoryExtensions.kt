package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
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
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getShowInstanceData(requestInstanceKey: InstanceKey): ShowInstanceViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowInstanceData")

    DomainThreadChecker.instance.requireDomainThread()

    /*
    val instanceKey = copiedTaskKeys[requestInstanceKey.taskKey]
            ?.let {
                val newScheduleKey = projectsFactory.getProjectForce(it.projectKey).convertScheduleKey(
                        deviceDbInfo.userInfo,
                        getTaskForce(requestInstanceKey.taskKey),
                        requestInstanceKey.scheduleKey,
                        false,
                )

                requestInstanceKey.copy(taskKey = it)
            }
            ?: requestInstanceKey
            */

    // todo migrate tasks
    val instanceKey = requestInstanceKey

    val task = getTaskForce(instanceKey.taskKey)

    val now = ExactTimeStamp.Local.now

    val instance = getInstance(instanceKey)
    val instanceDateTime = instance.instanceDateTime
    val parentInstance = instance.parentInstance

    var displayText = listOfNotNull(
        instance.getParentName().takeIf { it.isNotEmpty() },
        instanceDateTime.takeIf { instance.isRootInstance() }?.getDisplayText(),
    ).joinToString("\n\n")

    if (debugMode) {
        displayText += "\n\ntask key: " + instanceKey.taskKey
        displayText += "\ndate: " + instanceKey.scheduleKey.scheduleDate
        displayText += "\ncustom time: " + instanceKey.scheduleKey.scheduleTimePair.customTimeKey
        displayText += "\nnormal time: " + instanceKey.scheduleKey.scheduleTimePair.hourMinute
        displayText += "\nexists? " + instance.exists()
    }

    return ShowInstanceViewModel.Data(
        instance.name,
        instanceDateTime,
        instance.done != null,
        task.current(now),
        parentInstance == null,
        getGroupListData(instance, task, now),
        instance.getNotificationShown(shownFactory),
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

private fun DomainFactory.getGroupListData(
    instance: Instance,
    task: Task,
    now: ExactTimeStamp.Local,
): GroupListDataWrapper {
    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val instanceDatas = instance.getChildInstances()
        .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true)) }
        .map { childInstance ->
            val childTask = childInstance.task

            val children = getChildInstanceDatas(childInstance, now)

            GroupListDataWrapper.InstanceData(
                childInstance.done,
                childInstance.instanceKey,
                null,
                childInstance.name,
                childInstance.instanceDateTime.timeStamp,
                childInstance.instanceDateTime,
                childTask.current(now),
                childTask.isVisible(now),
                childInstance.isRootInstance(),
                childInstance.getCreateTaskTimePair(now, projectsFactory.privateProject),
                childTask.note,
                children,
                childTask.ordinal,
                childInstance.getNotificationShown(shownFactory),
                childTask.getImage(deviceDbInfo),
                childInstance.isAssignedToMe(now, myUserFactory.user),
                childInstance.getProjectInfo(now),
                childInstance.task
                    .project
                    .projectKey as? ProjectKey.Shared,
            )
        }

    return GroupListDataWrapper(
        customTimeDatas,
        instance.canAddSubtask(now),
        listOf(),
        task.note,
        instanceDatas,
        task.getImage(deviceDbInfo),
        instance.getProjectInfo(now),
    )
}