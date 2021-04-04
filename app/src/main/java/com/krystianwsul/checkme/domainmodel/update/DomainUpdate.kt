package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.ExactTimeStamp

interface DomainUpdate<T : Any> {

    val name: String

    fun doAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local): DomainUpdater.Result<T>
}