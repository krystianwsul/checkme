package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.ExactTimeStamp

class CompletableDomainUpdate(
    name: String,
    override val highPriority: Boolean,
    val action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Params,
) : AbstractCompletableDomainUpdate(name) {

    companion object {

        fun create(name: String, action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Params) =
            CompletableDomainUpdate(name, false, action)
    }

    override fun doCompletableAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local) = domainFactory.action(now)
}