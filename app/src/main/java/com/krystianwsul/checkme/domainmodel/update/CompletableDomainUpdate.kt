package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.ExactTimeStamp

class CompletableDomainUpdate(
        val action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Params,
) : DomainUpdate<Unit> {

    companion object {

        fun create(action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Params) =
                CompletableDomainUpdate(action)
    }

    override fun doAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local) =
            DomainUpdater.Result(Unit, action(domainFactory, now))

    fun perform(domainUpdater: DomainUpdater) = domainUpdater.performDomainUpdate(this).ignoreElement()!!
}