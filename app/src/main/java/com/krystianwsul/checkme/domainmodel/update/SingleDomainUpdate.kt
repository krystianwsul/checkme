package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.ExactTimeStamp

class SingleDomainUpdate<T : Any>(
        override val name: String,
        val action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Result<T>,
) : DomainUpdate<T> {

    companion object {

        fun <T : Any> create(name: String, action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Result<T>) =
                SingleDomainUpdate(name, action)
    }

    override fun doAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local) = action(domainFactory, now)

    fun perform(domainUpdater: DomainUpdater) = domainUpdater.performDomainUpdate(this)
}