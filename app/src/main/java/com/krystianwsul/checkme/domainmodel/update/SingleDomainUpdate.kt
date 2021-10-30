package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.ExactTimeStamp

class SingleDomainUpdate<T : Any>(
    name: String,
    private val action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Result<T>,
) : AbstractSingleDomainUpdate<T>(name) {

    companion object {

        fun <T : Any> create(name: String, action: DomainFactory.(ExactTimeStamp.Local) -> DomainUpdater.Result<T>) =
            SingleDomainUpdate(name, action)
    }

    override fun doAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local) = action(domainFactory, now)
}