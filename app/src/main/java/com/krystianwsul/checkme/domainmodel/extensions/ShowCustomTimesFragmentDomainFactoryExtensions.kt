package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.prettyPrint
import com.krystianwsul.checkme.viewmodels.ShowCustomTimesViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType

@Synchronized
fun DomainFactory.getShowCustomTimesData(): ShowCustomTimesViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowCustomTimesData")

    val now = ExactTimeStamp.now

    val entries = getCurrentRemoteCustomTimes(now).map {
        val days = it.hourMinutes
            .entries
            .groupBy { it.value }
            .mapValues { it.value.map { it.key } }
            .entries
            .sortedBy { it.key }

        val details = days.joinToString("; ") {
            it.value
                .toSet()
                .prettyPrint() + it.key
        }

        ShowCustomTimesViewModel.CustomTimeData(
            it.key,
            it.name,
            details
        )
    }.toMutableList()

    return ShowCustomTimesViewModel.Data(entries)
}

@Synchronized
fun DomainFactory.setCustomTimesCurrent(
    dataId: Int,
    source: SaveService.Source,
    customTimeIds: List<CustomTimeKey<ProjectType.Private>>,
    current: Boolean
) {
    MyCrashlytics.log("DomainFactory.setCustomTimesCurrent")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(customTimeIds.isNotEmpty())

    val now = ExactTimeStamp.now
    val endExactTimeStamp = now.takeUnless { current }

    for (customTimeId in customTimeIds) {
        val remotePrivateCustomTime = projectsFactory.privateProject.getCustomTime(customTimeId)

        remotePrivateCustomTime.endExactTimeStamp = endExactTimeStamp
    }

    save(dataId, source)
}