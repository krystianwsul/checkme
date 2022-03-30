package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.drag.DropParent
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.utils.time.toDateTimeSoy
import com.krystianwsul.checkme.viewmodels.ShowGroupViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.ProjectKey
import java.util.*

fun DomainFactory.getShowGroupData(
    parameters: ShowGroupActivity.Parameters,
    searchCriteria: SearchCriteria,
): ShowGroupViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowGroupData")

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val timeStamp = parameters.timeStamp
    val date = timeStamp.date
    val dayOfWeek = date.dayOfWeek
    val hourMinute = timeStamp.hourMinute

    val time = getCurrentRemoteCustomTimes().map { it as Time.Custom }
        .firstOrNull { it.getHourMinute(dayOfWeek) == hourMinute }
        ?: Time.Normal(hourMinute)

    val displayText = DateTime(date, time).getDisplayText()

    val (title, subtitle) = when (parameters) {
        is ShowGroupActivity.Parameters.Time -> displayText to null
        is ShowGroupActivity.Parameters.Project -> parameters.projectKey
            .let(projectsFactory::getProjectForce)
            .name to displayText
    }

    return ShowGroupViewModel.Data(
        title,
        subtitle,
        getGroupListData(timeStamp, now, parameters.projectKey, parameters.groupingMode, searchCriteria),
    )
}

private fun DomainFactory.getGroupListData(
    timeStamp: TimeStamp,
    now: ExactTimeStamp.Local,
    projectKey: ProjectKey.Shared?,
    groupingMode: GroupType.GroupingMode,
    searchCriteria: SearchCriteria,
): GroupListDataWrapper {
    val endCalendar = timeStamp.calendar.apply { add(Calendar.MINUTE, 1) }
    val endExactTimeStamp = ExactTimeStamp.Local(endCalendar.toDateTimeSoy()).toOffset()

    val rootInstances = getRootInstances(
        timeStamp.toLocalExactTimeStamp().toOffset(),
        endExactTimeStamp,
        now,
        searchCriteria,
        projectKey = projectKey,
    ).toList()

    val currentInstances = rootInstances.filter { it.instanceDateTime.timeStamp.compareTo(timeStamp) == 0 }

    val customTimeDatas = getCurrentRemoteCustomTimes().map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val includeProjectDetails = projectKey == null

    val instanceDescriptors = currentInstances.map { instance ->
        val childSearchCriteria = if (instance.task.matchesSearch(searchCriteria.search))
            searchCriteria.copy(search = null)
        else
            searchCriteria

        val (notDoneChildInstanceDescriptors, doneChildInstanceDescriptors) =
            getChildInstanceDatas(instance, now, childSearchCriteria)

        val instanceData = GroupListDataWrapper.InstanceData.fromInstance(
            instance,
            now,
            this,
            notDoneChildInstanceDescriptors,
            doneChildInstanceDescriptors,
        )

        GroupTypeFactory.InstanceDescriptor(
            instanceData,
            instance.instanceDateTime.toDateTimePair(),
            instance.groupByProject,
            instance,
        )
    }

    val (mixedInstanceDescriptors, doneInstanceDescriptors) = instanceDescriptors.splitDone()

    val dropParent = projectKey?.let { DropParent.Project(timeStamp, it) } ?: DropParent.TopLevel(true)

    return GroupListDataWrapper(
        customTimeDatas,
        null,
        listOf(),
        null,
        newMixedInstanceDataCollection(
            mixedInstanceDescriptors,
            GroupTypeFactory.SingleBridge.CompareBy.ORDINAL,
            groupingMode,
            false,
            includeProjectDetails,
        ),
        doneInstanceDescriptors.toDoneSingleBridges(false, includeProjectDetails),
        null,
        null,
        dropParent,
    )
}