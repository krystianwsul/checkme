package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.ExactTimeStamp

class SingleDomainUpdate<T : Any>(
        val action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Result<T>,
) : DomainUpdate<T> {

    companion object {

        fun <T : Any> create(action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Result<T>) =
                SingleDomainUpdate(action)
    }

    override fun doAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local) = action(domainFactory, now)

    fun perform(domainUpdater: DomainUpdater) = domainUpdater.performDomainUpdate(this)
}