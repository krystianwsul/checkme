package com.krystianwsul.checkme.domainmodel.extensions

import android.util.Log
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.SearchInstancesViewModel
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.toExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.QueryMatchAccumulator
import com.soywiz.klock.days

@Synchronized
fun DomainFactory.getSearchInstancesData(query: String, page: Int): SearchInstancesViewModel.Data {
    MyCrashlytics.log("DomainFactory.getSearchInstancesData")

    val now = ExactTimeStamp.now

    val customTimeDatas = getCurrentRemoteCustomTimes(now).map {
        GroupListDataWrapper.CustomTimeData(it.name, it.hourMinutes.toSortedMap())
    }

    var startExactTimeStamp: ExactTimeStamp? = null
    var endExactTimeStamp = now

    val instances = mutableMapOf<InstanceKey, Instance<*>>()

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

        instances += newInstances.associateBy { it.instanceKey }

        if (instances.size > (page + 1) * 20) break

        startExactTimeStamp = endExactTimeStamp

        endExactTimeStamp = endExactTimeStamp.toDateTimeSoy()
                .plus(1.days)
                .toExactTimeStamp()
    }

    val instanceDatas = instances.values
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
            .filter { it.matchesQuery(query) }

    val dataWrapper = GroupListDataWrapper(
            customTimeDatas,
            null,
            listOf(),
            null,
            instanceDatas,
            null
    )

    instanceDatas.forEach { it.instanceDataParent = dataWrapper }

    Log.e("asdf", "magic finishing search for $query $page")

    return SearchInstancesViewModel.Data(dataWrapper, hasMore)
}