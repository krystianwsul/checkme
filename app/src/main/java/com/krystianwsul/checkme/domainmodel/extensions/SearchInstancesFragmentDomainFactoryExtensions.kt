package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.instances.drag.DropParent
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.viewmodels.DomainQuery
import com.krystianwsul.checkme.viewmodels.SearchInstancesViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.FilterResult
import com.krystianwsul.common.firebase.models.filterSearch
import com.krystianwsul.common.locker.LockerManager
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey

fun DomainFactory.getSearchInstancesData(
    searchCriteria: SearchCriteria,
    page: Int,
): DomainQuery<SearchInstancesViewModel.Data> {
    MyCrashlytics.log("DomainFactory.getSearchInstancesData")

    DomainThreadChecker.instance.requireDomainThread()

    return LockerManager.setLocker { now ->
        getDomainResultInterrupting {
            val customTimeDatas = getCurrentRemoteCustomTimes().map {
                GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
            }

            val (cappedInstanceDescriptors, taskDatas, hasMore) = getCappedInstanceAndTaskDatas(now, searchCriteria, page)

            val dataWrapper = GroupListDataWrapper(
                customTimeDatas,
                null,
                taskDatas,
                null,
                newMixedInstanceDataCollection(cappedInstanceDescriptors),
                listOf(),
                null,
                null,
                DropParent.TopLevel(false),
            )

            SearchInstancesViewModel.Data(dataWrapper, hasMore, searchCriteria)
        }
    }
}

fun DomainFactory.getCappedInstanceAndTaskDatas(
    now: ExactTimeStamp.Local,
    searchCriteria: SearchCriteria,
    page: Int,
    projectKey: ProjectKey.Shared? = null,
): Triple<List<GroupTypeFactory.InstanceDescriptor>, List<GroupListDataWrapper.TaskData>, Boolean> {
    val includeProjectDetails = projectKey == null

    val (cappedInstanceDescriptors, hasMore) = searchInstances<GroupTypeFactory.InstanceDescriptor>(
        now,
        searchCriteria,
        page,
        projectKey,
    ) { instance, children -> instanceToGroupListData(instance, now, children) }

    val taskDatas = getUnscheduledTasks(projectKey)
        .asSequence()
        .filterSearch(searchCriteria.search)
        .map { (task, filterResult) ->
            val childQuery = if (filterResult == FilterResult.MATCHES) null else searchCriteria

            GroupListDataWrapper.TaskData(
                task.taskKey,
                task.name,
                getGroupListChildTaskDatas(task, now, childQuery),
                task.note,
                task.getImage(deviceDbInfo),
                task.isAssignedToMe(now, myUserFactory.user),
                task.getProjectInfo(now, includeProjectDetails),
                task.ordinal,
                task.canMigrateDescription(now),
            )
        }
        .toList()

    return Triple(cappedInstanceDescriptors, taskDatas, hasMore)
}