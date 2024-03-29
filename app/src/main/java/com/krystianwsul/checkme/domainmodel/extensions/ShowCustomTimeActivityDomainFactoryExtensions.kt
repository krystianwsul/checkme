package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
import com.krystianwsul.checkme.viewmodels.ShowCustomTimeViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.customtimes.UserCustomTimeJson
import com.krystianwsul.common.firebase.models.customtime.MyUserCustomTime
import com.krystianwsul.common.firebase.models.customtime.PrivateCustomTime
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getShowCustomTimeData(customTimeKey: CustomTimeKey): ShowCustomTimeViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowCustomTimeData")

    DomainThreadChecker.instance.requireDomainThread()

    val customTime = getCustomTime(customTimeKey)

    val hourMinutes = DayOfWeek.values().associateWith { customTime.getHourMinute(it) }

    return ShowCustomTimeViewModel.Data(customTimeKey, customTime.name, hourMinutes)
}

@CheckResult
fun DomainUpdater.updateCustomTime(
    notificationType: DomainListenerManager.NotificationType,
    customTimeId: CustomTimeKey,
    name: String,
    hourMinutes: Map<DayOfWeek, HourMinute>,
): Completable = CompletableDomainUpdate.create("updateCustomTime") { now ->
    check(name.isNotEmpty())

    val initialCustomTime = getCustomTime(customTimeId)

    val customTime = if (initialCustomTime is PrivateCustomTime) {
        migratePrivateCustomTime(initialCustomTime, now)
    } else {
        initialCustomTime as MyUserCustomTime
    }

    customTime.setName(name)

    for (dayOfWeek in DayOfWeek.values()) {
        val hourMinute = hourMinutes.getValue(dayOfWeek)

        customTime.setHourMinute(dayOfWeek, hourMinute)
    }

    DomainUpdater.Params(false, notificationType)
}.perform(this)

fun DomainFactory.migratePrivateCustomTime(
    privateCustomTime: PrivateCustomTime,
    now: ExactTimeStamp.Local,
): MyUserCustomTime {
    val myUserCustomTime = createUserCustomTime(privateCustomTime.name, privateCustomTime.hourMinutes, privateCustomTime)

    if (privateCustomTime.endExactTimeStamp == null) privateCustomTime.endExactTimeStamp = now

    return myUserCustomTime
}

@CheckResult
fun DomainUpdater.createCustomTime(
    notificationType: DomainListenerManager.NotificationType,
    name: String,
    hourMinutes: Map<DayOfWeek, HourMinute>,
): Single<CustomTimeKey.User> = SingleDomainUpdate.create("createCustomTime") {
    check(name.isNotEmpty())

    check(DayOfWeek.set == hourMinutes.keys)

    val customTime = createUserCustomTime(name, hourMinutes, null)

    DomainUpdater.Result(customTime.key, false, notificationType)
}.perform(this)

private fun DomainFactory.createUserCustomTime(
    name: String,
    hourMinutes: Map<DayOfWeek, HourMinute>,
    fromPrivateCustomTime: PrivateCustomTime?,
) = myUserFactory.user.newCustomTime(
    UserCustomTimeJson(
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
        fromPrivateCustomTime?.endExactTimeStamp?.long,
        fromPrivateCustomTime?.id?.value,
    )
)