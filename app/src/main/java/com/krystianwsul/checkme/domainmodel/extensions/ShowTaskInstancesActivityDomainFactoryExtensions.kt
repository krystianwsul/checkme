package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.gui.instances.drag.DropParent
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.viewmodels.DomainQuery
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.locker.LockerManager
import com.krystianwsul.common.utils.Endable

fun DomainFactory.getShowTaskInstancesData(
    parameters: ShowTaskInstancesActivity.Parameters,
    page: Int,
    searchCriteria: SearchCriteria,
): DomainQuery<ShowTaskInstancesViewModel.Data> {
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
            val notDoneInstanceDescriptors: List<GroupTypeFactory.InstanceDescriptor>
            val doneInstanceDescriptors: List<GroupTypeFactory.InstanceDescriptor>
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

                    notDoneInstanceDescriptors = pair.first.map {
                        val (notDoneChildInstanceDescriptors, doneChildInstanceDescriptors) = getChildInstanceDatas(it, now)

                        val instanceData = GroupListDataWrapper.InstanceData.fromInstance(
                            it,
                            now,
                            this,
                            notDoneChildInstanceDescriptors,
                            doneChildInstanceDescriptors,
                            false,
                        )

                        GroupTypeFactory.InstanceDescriptor(
                            instanceData,
                            it.instanceDateTime.toDateTimePair(),
                            it.groupByProject,
                            it,
                            null, // todo group later
                        )
                    }

                    doneInstanceDescriptors = emptyList()
                }
                is ShowTaskInstancesActivity.Parameters.Project -> {
                    val project = projectsFactory.getSharedProjectForce(parameters.projectKey)

                    parent = project

                    val triple = getCappedInstanceAndTaskDatas(now, searchCriteria, page, parameters.projectKey)

                    val splitInstanceDescriptors = triple.first.splitDone()

                    notDoneInstanceDescriptors = splitInstanceDescriptors.first
                    doneInstanceDescriptors = splitInstanceDescriptors.second
                    taskDatas = triple.second
                    hasMore = triple.third
                }
            }

            val dataWrapper = GroupListDataWrapper(
                customTimeDatas,
                parent.notDeleted,
                taskDatas,
                null,
                newMixedInstanceDataCollection(
                    notDoneInstanceDescriptors,
                    GroupTypeFactory.SingleBridge.CompareBy.TIMESTAMP,
                    parameters.groupingMode,
                    projectInfoMode = if (parameters.projectKey == null) ProjectInfoMode.Show else ProjectInfoMode.Hide,
                ),
                doneInstanceDescriptors.toDoneSingleBridges(projectInfoMode = ProjectInfoMode.Hide),
                null,
                null,
                DropParent.TopLevel(false),
                searchCriteria,
            )

            ShowTaskInstancesViewModel.Data(
                parameters.projectKey
                    ?.let(projectsFactory::getSharedProjectForce)
                    ?.name,
                dataWrapper,
                hasMore,
                searchCriteria,
            )
        }
    }
}