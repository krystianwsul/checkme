package com.krystianwsul.checkme.domainmodel.update

import androidx.annotation.CheckResult

abstract class AbstractSingleDomainUpdate<T : Any>(override val name: String) : DomainUpdate<T> {

    @CheckResult
    fun perform(domainUpdater: DomainUpdater) = domainUpdater.performDomainUpdate(this)
}