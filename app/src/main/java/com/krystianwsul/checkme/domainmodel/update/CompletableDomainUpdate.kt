package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainUpdater

class CompletableDomainUpdate(val action: DomainFactory.() -> DomainUpdater.Params) : DomainUpdate {

    companion object {

        fun create(action: DomainFactory.() -> DomainUpdater.Params) =
                CompletableDomainUpdate(action)
    }

    fun perform(domainUpdater: DomainUpdater) = domainUpdater.performDomainUpdate { domainFactory ->
        DomainUpdater.Result(Unit, action(domainFactory))
    }.ignoreElement()!!
}