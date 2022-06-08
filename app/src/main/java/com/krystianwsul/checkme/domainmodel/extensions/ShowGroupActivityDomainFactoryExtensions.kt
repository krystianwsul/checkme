package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
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
import com.krystianwsul.common.firebase.models.search.SearchContext
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
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
        is ShowGroupActivity.Parameters.Time -> Pair(displayText, null)
        is ShowGroupActivity.Parameters.Project -> Pair(
            parameters.projectKey
                .let(projectsFactory::getSharedProjectForce)
                .name,
            displayText,
        )
    }

    return ShowGroupViewModel.Data(title, subtitle, getGroupListData(parameters, now, searchCriteria))
}

private fun DomainFactory.getGroupListData(
    parameters: ShowGroupActivity.Parameters,
    now: ExactTimeStamp.Local,
    searchCriteria: SearchCriteria,
): GroupListDataWrapper {
    val endCalendar = parameters.timeStamp
        .calendar
        .apply { add(Calendar.MINUTE, 1) }

    val endExactTimeStamp = ExactTimeStamp.Local(endCalendar.toDateTimeSoy()).toOffset()

    val searchContext = SearchContext.startSearch(searchCriteria, now, myUserFactory.user)

    val instances = getRootInstances(
        parameters.timeStamp
            .toLocalExactTimeStamp()
            .toOffset(),
        endExactTimeStamp,
        now,
        searchContext,
        projectKey = parameters.projectKey,
    )
        // I'm really confused about why this filter would be necessary
        .filter { it.first.instanceDateTime.timeStamp.compareTo(parameters.timeStamp) == 0 }
        .let {
            if (parameters.showUngrouped)
                it
            else
                it.filter { it.first.groupByProject }
        }
        .toList()

    val customTimeDatas = getCurrentRemoteCustomTimes().map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val includeProjectDetails = parameters.projectKey == null

    val instanceDescriptors = searchContext.search {
        instances.map { (instance, filterResult) ->
            val (notDoneChildInstanceDescriptors, doneChildInstanceDescriptors) =
                getChildInstanceDatas(instance, now, getChildrenSearchContext(filterResult))

            val instanceData = GroupListDataWrapper.InstanceData.fromInstance(
                instance,
                now,
                this@getGroupListData,
                notDoneChildInstanceDescriptors,
                doneChildInstanceDescriptors,
                filterResult.matchesSearch,
            )

            GroupTypeFactory.InstanceDescriptor(
                instanceData,
                instance.instanceDateTime.toDateTimePair(),
                instance.groupByProject,
                instance,
                null, // todo group
            )
        }
    }

    val (mixedInstanceDescriptors, doneInstanceDescriptors) = instanceDescriptors.splitDone()

    val dropParent = parameters.projectKey
        ?.let { DropParent.Project(parameters.timeStamp, it) }
        ?: DropParent.TopLevel(true)

    return GroupListDataWrapper(
        customTimeDatas,
        null,
        listOf(),
        null,
        newMixedInstanceDataCollection(
            mixedInstanceDescriptors,
            GroupTypeFactory.SingleBridge.CompareBy.ORDINAL,
            parameters.groupingMode,
            false,
            includeProjectDetails,
        ),
        doneInstanceDescriptors.toDoneSingleBridges(false, includeProjectDetails),
        null,
        null,
        dropParent,
        searchCriteria,
    )
}