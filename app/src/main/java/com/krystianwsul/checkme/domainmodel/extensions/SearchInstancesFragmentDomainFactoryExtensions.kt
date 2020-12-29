package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.domainmodel.takeAndHasMore
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.checkme.viewmodels.SearchInstancesViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.FilterResult
import com.krystianwsul.common.firebase.models.filterQuery
import com.krystianwsul.common.locker.LockerManager

const val PAGE_SIZE = 20

fun DomainFactory.getSearchInstancesData(
        searchCriteria: SearchCriteria,
        page: Int,
): DomainResult<SearchInstancesViewModel.Data> = syncOnDomain {
    MyCrashlytics.log("DomainFactory.getSearchInstancesData")

    val desiredCount = (page + 1) * PAGE_SIZE

    LockerManager.setLocker { now ->
        getDomainResultInterrupting {
            val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
                GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
            }

            val (instances, hasMore) = getRootInstances(
                    null,
                    null,
                    now,
                    searchCriteria,
                    filterVisible = !debugMode
            ).takeAndHasMore(desiredCount)

            val instanceDatas = instances.map {
                val task = it.task

                /*
                We know this instance matches SearchCriteria.showAssignedToOthers.  If it also matches the query, we
                can skip filtering child instances, since showAssignedToOthers is meaningless for child instances.
                 */
                val childSearchCriteria = if (task.matchesQuery(searchCriteria.query)) null else searchCriteria

                val children = getChildInstanceDatas(it, now, childSearchCriteria, !debugMode)

                GroupListDataWrapper.InstanceData(
                        it.done,
                        it.instanceKey,
                        it.instanceDateTime.getDisplayText(),
                        it.name,
                        it.instanceDateTime.timeStamp,
                        it.instanceDateTime,
                        task.current(now),
                        it.canAddSubtask(now),
                        it.isRootInstance(),
                        it.getCreateTaskTimePair(ownerKey),
                        task.note,
                        children,
                        it.task.ordinal,
                        it.getNotificationShown(localFactory),
                        task.getImage(deviceDbInfo),
                        it.isGroupChild(),
                        it.isAssignedToMe(now, myUserFactory.user),
                        it.getProjectInfo(now),
                )
            }

            val cappedInstanceDatas = instanceDatas.sorted().take(desiredCount)

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