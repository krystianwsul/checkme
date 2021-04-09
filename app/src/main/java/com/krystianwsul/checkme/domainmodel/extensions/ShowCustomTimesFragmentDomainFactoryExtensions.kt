package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.utils.prettyPrint
import com.krystianwsul.checkme.viewmodels.ShowCustomTimesViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Completable

fun DomainFactory.getShowCustomTimesData(): ShowCustomTimesViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowCustomTimesData")

    DomainThreadChecker.instance.requireDomainThread()

    val now = ExactTimeStamp.Local.now

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

@CheckResult
fun DomainUpdater.setCustomTimesCurrent(
        notificationType: DomainListenerManager.NotificationType,
        customTimeIds: List<CustomTimeKey.Project<ProjectType.Private>>,
        current: Boolean,
): Completable = CompletableDomainUpdate.create("setCustomTimesCurrent") { now ->
    check(customTimeIds.isNotEmpty())

    val endExactTimeStamp = now.takeUnless { current }

    for (customTimeId in customTimeIds) {
        val remotePrivateCustomTime = projectsFactory.privateProject.getCustomTime(customTimeId)

        remotePrivateCustomTime.endExactTimeStamp = endExactTimeStamp
    }

    DomainUpdater.Params(false, notificationType)
}.perform(this)