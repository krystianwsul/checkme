package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory

class CompletableDomainUpdate(val action: DomainFactory.() -> DomainUpdater.Params) : DomainUpdate {

    companion object {

        fun create(action: DomainFactory.() -> DomainUpdater.Params) =
                CompletableDomainUpdate(action)
    }

    fun perform(domainUpdater: DomainUpdater) = domainUpdater.updateDomainCompletable(this)
}