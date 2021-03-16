package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.checkme.viewmodels.SearchInstancesViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.SchedulerType
import com.krystianwsul.common.firebase.SchedulerTypeHolder
import com.krystianwsul.common.firebase.models.FilterResult
import com.krystianwsul.common.firebase.models.filterQuery
import com.krystianwsul.common.locker.LockerManager

fun DomainFactory.getSearchInstancesData(
        searchCriteria: SearchCriteria,
        page: Int,
): DomainResult<SearchInstancesViewModel.Data> {
    MyCrashlytics.log("DomainFactory.getSearchInstancesData")

    SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

    return LockerManager.setLocker { now ->
        getDomainResultInterrupting {
            val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
                GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
            }

            val (cappedInstanceDatas, hasMore) = searchInstances(
                    now,
                    searchCriteria,
                    page,
                    null,
                    ::instanceToGroupListData,
            )

            val taskDatas = getUnscheduledTasks(now)
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
                                task.getProjectInfo(now),
                                task.ordinal,
                        )
                    }
                    .toList()

            val dataWrapper = GroupListDataWrapper(
                    customTimeDatas,
                    null,
                    taskDatas,
                    null,
                    cappedInstanceDatas,
                    null,
                    null
            )

            SearchInstancesViewModel.Data(dataWrapper, hasMore, searchCriteria)
        }
    }
}