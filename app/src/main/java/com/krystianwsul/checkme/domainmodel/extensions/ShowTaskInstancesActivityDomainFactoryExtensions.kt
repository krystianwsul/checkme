package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.domainmodel.getProjectInfo
import com.krystianwsul.checkme.domainmodel.takeAndHasMore
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.locker.LockerManager
import com.krystianwsul.common.utils.Endable
import com.krystianwsul.common.utils.ProjectKey

fun DomainFactory.getShowTaskInstancesData(
    parameters: ShowTaskInstancesActivity.Parameters,
    page: Int,
    searchCriteria: SearchCriteria,
): DomainResult<ShowTaskInstancesViewModel.Data> {
    MyCrashlytics.log("DomainFactory.getShowTaskInstancesData")

    DomainThreadChecker.instance.requireDomainThread()

    return LockerManager.setLocker { now ->
        getDomainResultInterrupting {
            val customTimeDatas = getCurrentRemoteCustomTimes().map {
                GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
            }

            val desiredCount = (page + 1) * SEARCH_PAGE_SIZE

            val parent: Endable
            val taskDatas: List<GroupListDataWrapper.TaskData>
            val instanceDatas: List<GroupListDataWrapper.InstanceData>
            val hasMore: Boolean
            when (parameters) {
                is ShowTaskInstancesActivity.Parameters.Task -> {
                    val task = getTaskForce(parameters.taskKey)

                    parent = task

                    val instanceSequence = task.getInstances(null, null, now)

                    taskDatas = listOf()

                    val pair = instanceSequence.filter {
                        it.isVisible(now, Instance.VisibilityOptions(hack24 = true))
                    }.takeAndHasMore(desiredCount)

                    hasMore = pair.second

                    instanceDatas = pair.first.map {
                        val children = getChildInstanceDatas(it, now, includeProjectInfo = true)

                        GroupListDataWrapper.InstanceData(
                            it.done,
                            it.instanceKey,
                            it.instanceDateTime.getDisplayText(),
                            it.name,
                            it.instanceDateTime.timeStamp,
                            it.instanceDateTime,
                            it.task.notDeleted,
                            it.canAddSubtask(now),
                            it.canMigrateDescription(now),
                            it.isRootInstance(),
                            it.getCreateTaskTimePair(projectsFactory.privateProject),
                            it.task.note,
                            children,
                            it.task.ordinal,
                            it.getNotificationShown(shownFactory),
                            it.task.getImage(deviceDbInfo),
                            it.isAssignedToMe(now, myUserFactory.user),
                            it.getProjectInfo(now, parameters.projectKey == null),
                            it.getProject().projectKey as? ProjectKey.Shared,
                        )
                    }
                }
                is ShowTaskInstancesActivity.Parameters.Project -> {
                    val project = projectsFactory.getProjectForce(parameters.projectKey)

                    parent = project

                    val triple = getCappedInstanceAndTaskDatas(now, searchCriteria, page, parameters.projectKey)

                    instanceDatas = triple.first
                    taskDatas = triple.second
                    hasMore = triple.third
                }
            }

            val dataWrapper = GroupListDataWrapper(
                customTimeDatas,
                parent.notDeleted,
                taskDatas,
                null,
                instanceDatas,
                null,
                null,
            )

            ShowTaskInstancesViewModel.Data(
                parameters.projectKey
                    ?.let(projectsFactory::getProjectForce)
                    ?.name,
                dataWrapper,
                hasMore,
                searchCriteria,
            )
        }
    }
}