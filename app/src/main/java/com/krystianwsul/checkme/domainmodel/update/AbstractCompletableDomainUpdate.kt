package com.krystianwsul.checkme.domainmodel.update

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.ExactTimeStamp

abstract class AbstractCompletableDomainUpdate(override val name: String) : DomainUpdate<Unit> {

    final override fun doAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local): DomainUpdater.Result<Unit> =
        DomainUpdater.Result(Unit, doCompletableAction(domainFactory, now))

    protected abstract fun doCompletableAction(domainFactory: DomainFactory, now: ExactTimeStamp.Local): DomainUpdater.Params

    @CheckResult
    fun perform(domainUpdater: DomainUpdater) = domainUpdater.performDomainUpdate(this).ignoreElement()
}