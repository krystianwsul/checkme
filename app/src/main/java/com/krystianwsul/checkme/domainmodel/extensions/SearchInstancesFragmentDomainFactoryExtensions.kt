package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.domainmodel.takeAndHasMore
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.checkme.viewmodels.SearchInstancesViewModel
import com.krystianwsul.common.locker.LockerManager

const val PAGE_SIZE = 20

fun DomainFactory.getSearchInstancesData(
        query: String,
        page: Int
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
                    query,
                    filterVisible = !debugMode
            ).takeAndHasMore(desiredCount)

            val instanceDatas = instances.map {
                val task = it.task

                val isRootTask = if (task.current(now)) task.isRootTask(now.toDateTime()) else null

                val children = getChildInstanceDatas(it, now, query)

                val instanceData = GroupListDataWrapper.InstanceData(
                        it.done,
                        it.instanceKey,
                        it.instanceDateTime.getDisplayText(),
                        it.name,
                        it.instanceDateTime.timeStamp,
                        task.current(now),
                        task.isVisible(now, false),
                        it.isRootInstance(now),
                        isRootTask,
                        it.exists(),
                        it.getCreateTaskTimePair(ownerKey),
                        task.note,
                        children,
                        it.task.ordinal,
                        it.getNotificationShown(localFactory),
                        task.getImage(deviceDbInfo),
                        it.isRepeatingGroupChild(now)
                )

                children.values.forEach { it.instanceDataParent = instanceData }

                instanceData
            }

            val cappedInstanceDatas = instanceDatas.sorted().take(desiredCount)

            val dataWrapper = GroupListDataWrapper(
                    customTimeDatas,
                    null,
                    listOf(),
                    null,
                    cappedInstanceDatas,
                    null
            )

            cappedInstanceDatas.forEach { it.instanceDataParent = dataWrapper }

            SearchInstancesViewModel.Data(dataWrapper, hasMore)
        }
    }
}