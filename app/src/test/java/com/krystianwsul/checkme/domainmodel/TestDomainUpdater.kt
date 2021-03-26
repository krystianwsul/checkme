package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import io.reactivex.rxjava3.core.Single


class TestDomainUpdater(domainFactory: DomainFactory) : DomainUpdater {

    private val domainFactorySingle = Single.just(domainFactory)

    override fun <T : Any> performDomainUpdate(action: (DomainFactory) -> DomainUpdater.Result<T>): Single<T> {
        val resultSingle = domainFactorySingle.flatMap { it.onReady() }
                .observeOnDomain()
                .doOnSuccess { check(!it.isSaved.value!!) }
                .map { it to action(it) }
                .cache()

        resultSingle.subscribe { (domainFactory, result) ->
            domainFactory.apply {
                result.params.notifierParams?.let(notifier::updateNotifications)

                result.params
                        .notificationType
                        ?.let(::save)

                result.params
                        .cloudParams
                        ?.let(::notifyCloud)
            }
        }

        return resultSingle.map { (_, result) -> result.data }
    }
}