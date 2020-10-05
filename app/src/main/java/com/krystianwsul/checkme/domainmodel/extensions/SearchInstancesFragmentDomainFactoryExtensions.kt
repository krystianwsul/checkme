package com.krystianwsul.checkme.domainmodel.extensions

import android.util.Log
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.SearchInstancesViewModel
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.locker.LockerManager
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.toExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.QueryMatchAccumulator
import com.krystianwsul.common.utils.TimeLogger
import com.soywiz.klock.days

@Synchronized
fun DomainFactory.getSearchInstancesData(query: String, page: Int): SearchInstancesViewModel.Data {
    MyCrashlytics.log("DomainFactory.getSearchInstancesData")

    TimeLogger.clear() // todo search
    Task.permutations.clear() // todo search

    return LockerManager.setLocker { now ->
        val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
            GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
        }

        var startExactTimeStamp: ExactTimeStamp? = null
        var endExactTimeStamp = now

        val instanceKeys = mutableSetOf<InstanceKey>()
        val instanceDatas = mutableListOf<GroupListDataWrapper.InstanceData>()

        var step = 1

        var hasMore = true
        while (hasMore) {
            val queryMatchAccumulator = QueryMatchAccumulator(query)

            val newInstances = getRootInstances(
                    startExactTimeStamp,
                    endExactTimeStamp,
                    now,
                    queryMatchAccumulator
            )

            if (!queryMatchAccumulator.hasMore) hasMore = false

            val tracker2p5 = TimeLogger.start("filter instances for query")
            val x = newInstances.filter { it.instanceKey !in instanceKeys }
                    .filter { it.matchesQuery(now, query) }
            tracker2p5.stop()

            val tracker3 = TimeLogger.start("make instanceDatas")
            val newInstanceDatas = x
                    .map {
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

            instanceDatas += newInstanceDatas
            instanceKeys += newInstanceDatas.map { it.instanceKey }

            if (instanceDatas.size > (page + 1) * 20) break

            startExactTimeStamp = endExactTimeStamp

            step *= 2

            endExactTimeStamp = endExactTimeStamp.toDateTimeSoy()
                    .plus(step.days)
                    .toExactTimeStamp()

            tracker3.stop()
        }

        val dataWrapper = GroupListDataWrapper(
                customTimeDatas,
                null,
                listOf(),
                null,
                instanceDatas,
                null
        )

        instanceDatas.forEach { it.instanceDataParent = dataWrapper }

        TimeLogger.print()

        Log.e("asdf", "magic entries: " + Task.permutations.size)
        Log.e("asdf", "magic calls: " + Task.permutations.values.sum())

        SearchInstancesViewModel.Data(dataWrapper, hasMore)
    }
}