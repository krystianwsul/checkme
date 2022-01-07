package com.krystianwsul.checkme.domainmodel.updates

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.AbstractCompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey

class SetOrdinalDomainUpdate(
    private val notificationType: DomainListenerManager.NotificationType,
    private val taskKey: TaskKey,
    private val ordinal: Double,
) : AbstractCompletableDomainUpdate("setOrdinal") {

    override fun doCompletableAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local): DomainUpdater.Params {
        val task = domainFactory.getTaskForce(taskKey)

        task.ordinal = ordinal

        return DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(task.project))
    }
}