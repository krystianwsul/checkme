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
import com.krystianwsul.common.firebase.models.PrivateCustomTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
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

        ShowCustomTimesViewModel.CustomTimeData(it.key, it.name, details)
    }.toMutableList()

    return ShowCustomTimesViewModel.Data(entries)
}

@CheckResult
fun DomainUpdater.setCustomTimesCurrent(
        notificationType: DomainListenerManager.NotificationType,
        customTimeKeys: List<CustomTimeKey>,
        current: Boolean,
): Completable = CompletableDomainUpdate.create("setCustomTimesCurrent") { now ->
    check(customTimeKeys.isNotEmpty())

    val endExactTimeStamp = now.takeUnless { current }

    customTimeKeys.map(::getCustomTime).forEach {
        when (it) {
            is PrivateCustomTime -> it.endExactTimeStamp = endExactTimeStamp
            is Time.Custom.User -> it.endExactTimeStamp = endExactTimeStamp
            else -> throw IllegalArgumentException()
        }
    }

    DomainUpdater.Params(false, notificationType)
}.perform(this)