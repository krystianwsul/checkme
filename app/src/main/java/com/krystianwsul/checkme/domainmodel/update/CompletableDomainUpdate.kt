package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.ExactTimeStamp

class CompletableDomainUpdate(
        val action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Params,
) : DomainUpdate {

    companion object {

        fun create(action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Params) =
                CompletableDomainUpdate(action)
    }

    fun perform(domainUpdater: DomainUpdater) = domainUpdater.updateDomainCompletable(this)
}