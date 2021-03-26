package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import io.reactivex.rxjava3.core.Single


class TestDomainUpdater(private val domainFactory: DomainFactory) : DomainUpdater() {

    override fun <T : Any> performDomainUpdate(action: (DomainFactory) -> Result<T>): Single<T> {
        val (data, params) = action(domainFactory)

        domainFactory.apply {
            params.notifierParams?.let(notifier::updateNotifications)

            params.notificationType?.let(::save)

            params.cloudParams?.let(::notifyCloud)
        }

        return Single.just(data)
    }
}