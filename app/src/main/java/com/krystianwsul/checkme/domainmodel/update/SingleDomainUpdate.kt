package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory

class SingleDomainUpdate<T : Any>(val action: DomainFactory.() -> DomainUpdater.Result<T>) : DomainUpdate {

    companion object {

        fun <T : Any> create(action: DomainFactory.() -> DomainUpdater.Result<T>) =
                SingleDomainUpdate(action)
    }

    fun perform(domainUpdater: DomainUpdater) = domainUpdater.updateDomainSingle(this)
}