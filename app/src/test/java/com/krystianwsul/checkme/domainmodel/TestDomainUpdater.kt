package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.core.Single


class TestDomainUpdater(
        private val domainFactory: DomainFactory,
        private val now: ExactTimeStamp.Local,
) : DomainUpdater() {

    override fun <T : Any> performDomainUpdate(
            action: (DomainFactory, ExactTimeStamp.Local) -> Result<T>,
    ): Single<T> {
        val (data, params) = action(domainFactory, now)

        domainFactory.apply {
            params.notifierParams
                    ?.fix(now)
                    ?.let(notifier::updateNotifications)

            params.notificationType?.let(::save)

            params.cloudParams?.let(::notifyCloud)
        }

        return Single.just(data)
    }
}