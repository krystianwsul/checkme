package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.ExactTimeStamp

class CompletableDomainUpdate(
        override val name: String,
        val action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Params,
) : DomainUpdate<Unit> {

    companion object {

        fun create(name: String, action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Params) =
                CompletableDomainUpdate(name, action)
    }

    override fun doAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local) =
            DomainUpdater.Result(Unit, action(domainFactory, now))

    fun perform(domainUpdater: DomainUpdater) = domainUpdater.performDomainUpdate(this).ignoreElement()!!
}