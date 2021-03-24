package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.DomainUpdater
import com.krystianwsul.checkme.viewmodels.ShowCustomTimeViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getShowCustomTimeData(customTimeKey: CustomTimeKey.Private): ShowCustomTimeViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowCustomTimeData")

    DomainThreadChecker.instance.requireDomainThread()

    val customTime = projectsFactory.privateProject.getCustomTime(customTimeKey)

    val hourMinutes = DayOfWeek.values().associate { it to customTime.getHourMinute(it) }

    return ShowCustomTimeViewModel.Data(customTimeKey, customTime.name, hourMinutes)
}

@CheckResult
fun DomainUpdater.updateCustomTime(
        notificationType: DomainListenerManager.NotificationType,
        customTimeId: CustomTimeKey.Private,
        name: String,
        hourMinutes: Map<DayOfWeek, HourMinute>,
) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.updateCustomTime")

    check(name.isNotEmpty())

    val customTime = projectsFactory.privateProject.getCustomTime(customTimeId)

    customTime.setName(this, name)

    for (dayOfWeek in DayOfWeek.values()) {
        val hourMinute = hourMinutes.getValue(dayOfWeek)

        if (hourMinute != customTime.getHourMinute(dayOfWeek))
            customTime.setHourMinute(this, dayOfWeek, hourMinute)
    }

    DomainUpdater.Params(notificationType = notificationType)
}

@CheckResult
fun DomainUpdater.createCustomTime(
        notificationType: DomainListenerManager.NotificationType,
        name: String,
        hourMinutes: Map<DayOfWeek, HourMinute>,
): Single<CustomTimeKey.Private> = updateDomainSingle {
    MyCrashlytics.log("DomainFactory.createCustomTime")

    check(name.isNotEmpty())

    check(DayOfWeek.values().all { hourMinutes[it] != null })

    val customTimeJson = PrivateCustomTimeJson(
            name,
            hourMinutes.getValue(DayOfWeek.SUNDAY).hour,
            hourMinutes.getValue(DayOfWeek.SUNDAY).minute,
            hourMinutes.getValue(DayOfWeek.MONDAY).hour,
            hourMinutes.getValue(DayOfWeek.MONDAY).minute,
            hourMinutes.getValue(DayOfWeek.TUESDAY).hour,
            hourMinutes.getValue(DayOfWeek.TUESDAY).minute,
            hourMinutes.getValue(DayOfWeek.WEDNESDAY).hour,
            hourMinutes.getValue(DayOfWeek.WEDNESDAY).minute,
            hourMinutes.getValue(DayOfWeek.THURSDAY).hour,
            hourMinutes.getValue(DayOfWeek.THURSDAY).minute,
            hourMinutes.getValue(DayOfWeek.FRIDAY).hour,
            hourMinutes.getValue(DayOfWeek.FRIDAY).minute,
            hourMinutes.getValue(DayOfWeek.SATURDAY).hour,
            hourMinutes.getValue(DayOfWeek.SATURDAY).minute,
            true,
    )

    val remoteCustomTime = projectsFactory.privateProject.newRemoteCustomTime(customTimeJson)

    DomainUpdater.Result(remoteCustomTime.key, notificationType = notificationType)
}