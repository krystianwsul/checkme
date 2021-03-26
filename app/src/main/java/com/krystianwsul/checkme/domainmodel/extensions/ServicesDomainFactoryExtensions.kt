package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.Notifier
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.utils.time.calendar
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import java.util.*

fun DomainUpdater.setInstanceAddHourService(instanceKey: InstanceKey): Completable = CompletableDomainUpdate.create {
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

    DomainUpdater.Params(
            Notifier.Params(now, "setInstanceAddHourService ${instance.name}"),
            DomainListenerManager.NotificationType.All,
            DomainFactory.CloudParams(instance.task.project),
    )
}.perform(this)

fun DomainUpdater.setInstanceNotificationDoneService(instanceKey: InstanceKey): Completable =
        CompletableDomainUpdate.create {
            MyCrashlytics.log("DomainFactory.setInstanceNotificationDone")

            val instance = getInstance(instanceKey)
            Preferences.tickLog.logLineHour("DomainFactory: setting ${instance.name} done")

            val now = ExactTimeStamp.Local.now

            instance.setDone(localFactory, true, now)
            instance.setNotificationShown(localFactory, false)

            DomainUpdater.Params(
                    Notifier.Params(now, "setInstanceNotificationDone ${instance.name}"),
                    DomainListenerManager.NotificationType.All,
                    DomainFactory.CloudParams(instance.task.project),
            )
        }.perform(this)

fun DomainUpdater.setInstancesNotifiedService(instanceKeys: List<InstanceKey>): Completable =
        CompletableDomainUpdate.create {
            MyCrashlytics.log("DomainFactory.setInstancesNotified")

            check(instanceKeys.isNotEmpty())

            instanceKeys.forEach(::setInstanceNotified)

            DomainUpdater.Params(notificationType = DomainListenerManager.NotificationType.All)
        }.perform(this)

fun DomainUpdater.setTaskImageUploadedService(
        taskKey: TaskKey,
        imageUuid: String,
): Completable = CompletableDomainUpdate.create {
    MyCrashlytics.log("DomainFactory.clearProjectEndTimeStamps")

    val task = getTaskIfPresent(taskKey)
    if (task?.getImage(deviceDbInfo) != ImageState.Local(imageUuid)) {
        DomainUpdater.Params()
    } else {
        task.setImage(deviceDbInfo, ImageState.Remote(imageUuid))

        DomainUpdater.Params(
                notificationType = DomainListenerManager.NotificationType.All,
                cloudParams = DomainFactory.CloudParams(task.project),
        )
    }
}.perform(this)