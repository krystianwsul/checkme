package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.utils.filterNotNull
import io.reactivex.rxjava3.core.Single


object AndroidDomainUpdater : DomainUpdater() {

    private val domainFactorySingle = DomainFactory.instanceRelay
            .filterNotNull()
            .firstOrError()

    override fun <T : Any> performDomainUpdate(action: (DomainFactory) -> Result<T>): Single<T> {
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