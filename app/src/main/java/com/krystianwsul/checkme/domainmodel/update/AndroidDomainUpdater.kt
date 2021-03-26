package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.core.Single


object AndroidDomainUpdater : DomainUpdater() {

    private val domainFactorySingle = DomainFactory.instanceRelay
            .filterNotNull()
            .firstOrError()

    override fun <T : Any> performDomainUpdate(action: (DomainFactory, ExactTimeStamp.Local) -> Result<T>): Single<T> {
        val resultSingle = domainFactorySingle.flatMap { it.onReady() }
                .observeOnDomain()
                .doOnSuccess { check(!it.isSaved.value!!) }
                .map {
                    val now = ExactTimeStamp.Local.now
                    Triple(it, action(it, now), now)
                }
                .cache()

        resultSingle.subscribe { (domainFactory, result, now) ->
            domainFactory.apply {
                result.params.notifierParams
                        ?.fix(now)
                        ?.let(notifier::updateNotifications)

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