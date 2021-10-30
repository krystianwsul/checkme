package com.krystianwsul.checkme.domainmodel.update

abstract class AbstractSingleDomainUpdate<T : Any>(override val name: String) : DomainUpdate<T> {

    fun perform(domainUpdater: DomainUpdater) = domainUpdater.performDomainUpdate(this)
}