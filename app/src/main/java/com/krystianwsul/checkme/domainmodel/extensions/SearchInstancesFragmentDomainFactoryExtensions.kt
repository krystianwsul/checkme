package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.checkme.viewmodels.SearchInstancesViewModel
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.interrupt.throwIfInterrupted
import com.krystianwsul.common.locker.LockerManager
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.toExactTimeStamp
import com.krystianwsul.common.utils.QueryMatchAccumulator
import com.soywiz.klock.days

private const val PAGE_SIZE = 20

@Synchronized
fun DomainFactory.getSearchInstancesData(query: String, page: Int): DomainResult<SearchInstancesViewModel.Data> {
    MyCrashlytics.log("DomainFactory.getSearchInstancesData")

    val allPagesSize = (page + 1) * PAGE_SIZE

    return LockerManager.setLocker { now ->
        getDomainResultInterrupting {
            val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
                GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
            }

            var startExactTimeStamp: ExactTimeStamp? = null
            var endExactTimeStamp = now

            val instances = mutableListOf<Instance<*>>()

            var step = 1

            var hasMore = true
            while (hasMore) {
                throwIfInterrupted()

                val queryMatchAccumulator = QueryMatchAccumulator(query)

                val newInstances = getRootInstances(
                        startExactTimeStamp,
                        endExactTimeStamp,
                        now,
                        queryMatchAccumulator
                ).toList()

                instances += newInstances

                if (!queryMatchAccumulator.hasMore) hasMore = false

                if (instances.size > allPagesSize) break

                startExactTimeStamp = endExactTimeStamp

                if (newInstances.size < PAGE_SIZE) step *= 2

                endExactTimeStamp = endExactTimeStamp.toDateTimeSoy()
                        .plus(step.days)
                        .toExactTimeStamp()
            }

            val instanceDatas = instances.map {
                val task = it.task

                val isRootTask = if (task.current(now)) task.isRootTask(now) else null

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

            val cappedInstanceDatas = instanceDatas.sorted().take(allPagesSize)

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