package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey

@Synchronized
fun DomainFactory.setInstancesDone(
    dataId: Int,
    source: SaveService.Source,
    instanceKeys: List<InstanceKey>,
    done: Boolean
): ExactTimeStamp {
    MyCrashlytics.log("DomainFactory.setInstancesDone")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.now

    val instances = instanceKeys.map(this::getInstance)

    instances.forEach { it.setDone(localFactory, done, now) }

    val remoteProjects = instances.map { it.project }.toSet()

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(remoteProjects)

    return now
}