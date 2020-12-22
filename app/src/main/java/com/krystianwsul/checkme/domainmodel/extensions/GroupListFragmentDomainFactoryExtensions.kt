package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey

fun DomainFactory.setInstancesDone(
        dataId: Int,
        source: SaveService.Source,
        instanceKeys: List<InstanceKey>,
        done: Boolean,
): ExactTimeStamp.Local = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setInstancesDone")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.Local.now

    val instances = instanceKeys.map(this::getInstance)

    instances.forEach { it.setDone(localFactory, done, now) }

    val remoteProjects = instances.map { it.task.project }.toSet()

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(remoteProjects)

    now
}

fun DomainFactory.undoSetInstancesDateTime(
        dataId: Int,
        source: SaveService.Source,
        editInstancesUndoData: DomainFactory.EditInstancesUndoData
) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.undoSetInstancesDateTime")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(editInstancesUndoData.data.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    val instances = editInstancesUndoData.data.map { (instanceKey, dateTime) ->
        getInstance(instanceKey).apply {
            setInstanceDateTime(localFactory, ownerKey, dateTime, now)
        }
    }

    val projects = instances.map { it.task.project }.toSet()

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(projects)
}