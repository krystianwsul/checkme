package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.gui.instances.drag.DropParent
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.viewmodels.ShowNotificationGroupViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.search.SearchContext
import com.krystianwsul.common.firebase.models.search.filterSearchCriteria
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey

fun DomainFactory.getShowNotificationGroupData(
    instanceKeys: Set<InstanceKey>,
    searchCriteria: SearchCriteria,
): ShowNotificationGroupViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowNotificationGroupData")

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

    val queriedInstances = instanceKeys.map { getInstance(it) }.filter { it.isRootInstance() }
    val notificationInstances = Notifier.getNotificationInstances(this, now)

    val instances = (queriedInstances + notificationInstances).distinct().sortedBy { it.instanceDateTime }

    val customTimeDatas = getCurrentRemoteCustomTimes().map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    val searchContext = SearchContext.startSearch(searchCriteria)

    val instanceDescriptors = instances.asSequence()
        .filterSearchCriteria(searchContext, now, myUserFactory.user, false)
        .map { it.first } // todo sequence
        .map { instance ->
            val matchResult = instance.task.getMatchResult(searchContext.searchCriteria.search)

            val childSearchContext = searchContext.getChildrenSearchContext(matchResult)

            val (notDoneChildInstanceDescriptors, doneChildInstanceDescriptors) =
                getChildInstanceDatas(instance, now, childSearchContext)

            val instanceData = GroupListDataWrapper.InstanceData.fromInstance(
                instance,
                now,
                this,
                notDoneChildInstanceDescriptors,
                doneChildInstanceDescriptors,
                matchResult.matches,
            )

            GroupTypeFactory.InstanceDescriptor(
                instanceData,
                instance.instanceDateTime.toDateTimePair(),
                instance.groupByProject,
                instance,
            )
        }
        .toList()

    val (mixedInstanceDatas, doneInstanceDatas) = instanceDescriptors.splitDone()

    val dataWrapper = GroupListDataWrapper(
        customTimeDatas,
        null,
        listOf(),
        null,
        newMixedInstanceDataCollection(
            mixedInstanceDatas,
            GroupTypeFactory.SingleBridge.CompareBy.TIMESTAMP,
            GroupType.GroupingMode.Projects,
        ),
        doneInstanceDatas.toDoneSingleBridges(),
        null,
        null,
        DropParent.TopLevel(false),
    )

    return ShowNotificationGroupViewModel.Data(dataWrapper)
}