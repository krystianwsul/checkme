package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.DomainUpdater
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import java.util.*

fun DomainUpdater.setInstanceAddHourService(instanceKey: InstanceKey) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.setInstanceAddHourService")

    val instance = getInstance(instanceKey)
    Preferences.tickLog.logLineHour("DomainFactory: adding hour to ${instance.name}")

    val now = ExactTimeStamp.Local.now
    val calendar = now.calendar.apply { add(Calendar.HOUR_OF_DAY, 1) }

    val date = Date(calendar.toDateTimeTz())
    val hourMinute = HourMinute(calendar.toDateTimeTz())

    instance.setInstanceDateTime(
            localFactory,
            ownerKey,
            DateTime(date, Time.Normal(hourMinute)),
    )
    instance.setNotificationShown(localFactory, false)

    notifier.updateNotifications(now, sourceName = "setInstanceAddHourService ${instance.name}")

    DomainUpdater.Params(DomainListenerManager.NotificationType.All, DomainFactory.CloudParams(instance.task.project))
}

fun DomainUpdater.setInstanceNotificationDoneService(instanceKey: InstanceKey) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.setInstanceNotificationDone")

    val instance = getInstance(instanceKey)
    Preferences.tickLog.logLineHour("DomainFactory: setting ${instance.name} done")

    val now = ExactTimeStamp.Local.now

    instance.setDone(localFactory, true, now)
    instance.setNotificationShown(localFactory, false)

    notifier.updateNotifications(now, sourceName = "setInstanceNotificationDone ${instance.name}")

    DomainUpdater.Params(
            DomainListenerManager.NotificationType.All,
            DomainFactory.CloudParams(instance.task.project),
    )
}

fun DomainFactory.setInstancesNotifiedService(instanceKeys: List<InstanceKey>) {
    MyCrashlytics.log("DomainFactory.setInstancesNotified")

    check(instanceKeys.isNotEmpty())

    for (instanceKey in instanceKeys)
        setInstanceNotified(instanceKey)

    save(DomainListenerManager.NotificationType.All)
}

fun DomainUpdater.setTaskImageUploadedService(taskKey: TaskKey, imageUuid: String) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.clearProjectEndTimeStamps")

    val task = getTaskIfPresent(taskKey)
    if (task?.getImage(deviceDbInfo) != ImageState.Local(imageUuid)) {
        DomainUpdater.Params()
    } else {
        task.setImage(deviceDbInfo, ImageState.Remote(imageUuid))

        DomainUpdater.Params(DomainListenerManager.NotificationType.All, DomainFactory.CloudParams(task.project))
    }
}