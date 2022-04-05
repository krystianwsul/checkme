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
import com.krystianwsul.common.firebase.models.search.SearchContext
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
                newMixedInstanceDataCollection(cappedInstanceDescriptors, GroupTypeFactory.SingleBridge.CompareBy.TIMESTAMP),
                listOf(),
                null,
                null,
                DropParent.TopLevel(false),
                searchCriteria,
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

    val searchContext = SearchContext.startSearch(searchCriteria, now, myUserFactory.user)

    val (cappedInstanceDescriptors, hasMore) = searchInstances<GroupTypeFactory.InstanceDescriptor>(
        now,
        searchContext,
        page,
        projectKey,
    ) { instance, children, filterResult -> instanceToGroupListData(instance, now, children, filterResult.matchesSearch) }

    val taskDatas = searchContext.search {
        getUnscheduledTasks(projectKey)
            .asSequence()
            .filterSearch() // this isn't filterSearchCriteria because these are note tasks, so the other checks aren't relevant
            .map { (task, filterResult) ->
                GroupListDataWrapper.TaskData(
                    task.taskKey,
                    task.name,
                    getGroupListChildTaskDatas(task, now, getChildrenSearchContext(filterResult)),
                    task.note,
                    task.getImage(deviceDbInfo),
                    task.getProjectInfo(includeProjectDetails),
                    task.ordinal,
                    task.canMigrateDescription(now),
                    filterResult.matchesSearch,
                )
            }
            .toList()
    }

    return Triple(cappedInstanceDescriptors, taskDatas, hasMore)
}