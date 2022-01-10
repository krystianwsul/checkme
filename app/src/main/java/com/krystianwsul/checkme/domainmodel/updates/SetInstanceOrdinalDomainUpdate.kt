package com.krystianwsul.checkme.domainmodel.updates

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.domainmodel.update.AbstractCompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.Ordinal

class SetInstanceOrdinalDomainUpdate(
    private val notificationType: DomainListenerManager.NotificationType,
    private val instanceKey: InstanceKey,
    private val ordinal: Ordinal,
    private val newParentInfo: Instance.NewParentInfo,
) : AbstractCompletableDomainUpdate("setOrdinalInstance") {

    override fun doCompletableAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local): DomainUpdater.Params {
        val instance = domainFactory.getInstance(instanceKey)

        instance.setOrdinal(ordinal, newParentInfo)

        //todo ordinal
//        return DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(instance.getProject()))
        return DomainUpdater.Params(
            Notifier.Params(),
            DomainFactory.SaveParams(notificationType, true),
            DomainFactory.CloudParams(instance.getProject())
        )
    }
}