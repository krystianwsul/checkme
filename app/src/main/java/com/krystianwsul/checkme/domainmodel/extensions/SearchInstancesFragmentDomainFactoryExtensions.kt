package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.checkme.viewmodels.SearchInstancesViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.FilterResult
import com.krystianwsul.common.firebase.models.filterQuery
import com.krystianwsul.common.locker.LockerManager
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey

fun DomainFactory.getSearchInstancesData(
    searchCriteria: SearchCriteria,
    page: Int,
): DomainResult<SearchInstancesViewModel.Data> {
    MyCrashlytics.log("DomainFactory.getSearchInstancesData")

    DomainThreadChecker.instance.requireDomainThread()

    return LockerManager.setLocker { now ->
        getDomainResultInterrupting {
            val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
                GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
            }

            val (cappedInstanceDatas, taskDatas, hasMore) = getCappedInstanceAndTaskDatas(now, searchCriteria, page)

            val dataWrapper = GroupListDataWrapper(
                customTimeDatas,
                null,
                taskDatas,
                null,
                cappedInstanceDatas,
                null,
                null,
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
): Triple<List<GroupListDataWrapper.InstanceData>, List<GroupListDataWrapper.TaskData>, Boolean> {
    val includeProjectInfo = projectKey == null

    val (cappedInstanceDatas, hasMore) = searchInstances<GroupListDataWrapper.InstanceData>(
        now,
        searchCriteria,
        page,
        projectKey,
    ) { instance, children -> instanceToGroupListData(instance, now, children, includeProjectInfo) }

    val taskDatas = getUnscheduledTasks(now, projectKey)
        .asSequence()
        .filterQuery(searchCriteria.query)
        .map { (task, filterResult) ->
            val childQuery = if (filterResult == FilterResult.MATCHES) null else searchCriteria

            GroupListDataWrapper.TaskData(
                task.taskKey,
                task.name,
                getGroupListChildTaskDatas(task, now, childQuery),
                task.startExactTimeStamp,
                task.note,
                task.getImage(deviceDbInfo),
                task.isAssignedToMe(now, myUserFactory.user),
                task.getProjectInfo(now, includeProjectInfo),
                task.ordinal,
            )
        }
        .toList()

    return Triple(cappedInstanceDatas, taskDatas, hasMore)
}