package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.utils.time.toDateTimeSoy
import com.krystianwsul.checkme.viewmodels.ShowGroupViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.ProjectKey
import java.util.*

fun DomainFactory.getShowGroupData(parameters: ShowGroupActivity.Parameters): ShowGroupViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowGroupData")

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val timeStamp = parameters.timeStamp
    val date = timeStamp.date
    val dayOfWeek = date.dayOfWeek
    val hourMinute = timeStamp.hourMinute

    val time = getCurrentRemoteCustomTimes(now).map { it as Time.Custom }
        .firstOrNull { it.getHourMinute(dayOfWeek) == hourMinute }
        ?: Time.Normal(hourMinute)

    val displayText = DateTime(date, time).getDisplayText()

    val (title, subtitle) = when (parameters) {
        is ShowGroupActivity.Parameters.Time -> displayText to null
        is ShowGroupActivity.Parameters.Project -> parameters.projectKey
            .let(projectsFactory::getProjectForce)
            .name to displayText
    }

    return ShowGroupViewModel.Data(title, subtitle, getGroupListData(timeStamp, now, parameters.projectKey))
}

private fun DomainFactory.getGroupListData(
    timeStamp: TimeStamp,
    now: ExactTimeStamp.Local,
    projectKey: ProjectKey.Shared? = null,
): GroupListDataWrapper {
    val endCalendar = timeStamp.calendar.apply { add(Calendar.MINUTE, 1) }
    val endExactTimeStamp = ExactTimeStamp.Local(endCalendar.toDateTimeSoy()).toOffset()

    val rootInstances = getRootInstances(
        timeStamp.toLocalExactTimeStamp().toOffset(),
        endExactTimeStamp,
        now,
        projectKey = projectKey,
    ).toList()

    val currentInstances = rootInstances.filter { it.instanceDateTime.timeStamp.compareTo(timeStamp) == 0 }

    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(
            it.name,
            it.hourMinutes.toSortedMap()
        )
    }

    val includeProjectInfo = projectKey == null

    val instanceDatas = currentInstances.map { instance ->
        val task = instance.task

        val children = getChildInstanceDatas(instance, now, includeProjectInfo = includeProjectInfo)

        GroupListDataWrapper.InstanceData(
            instance.done,
            instance.instanceKey,
            null,
                instance.name,
                instance.instanceDateTime.timeStamp,
                instance.instanceDateTime,
            task.current(now),
            instance.canAddSubtask(now),
            instance.isRootInstance(),
            instance.getCreateTaskTimePair(now, projectsFactory.privateProject),
            task.note,
            children,
            task.ordinal,
            instance.getNotificationShown(localFactory),
            task.getImage(deviceDbInfo),
            instance.isAssignedToMe(now, myUserFactory.user),
            instance.getProjectInfo(now, includeProjectInfo),
            instance.task
                .project
                .projectKey as? ProjectKey.Shared,
        )
    }

    return GroupListDataWrapper(
            customTimeDatas,
            null,
            listOf(),
            null,
            instanceDatas,
            null,
            null
    )
}