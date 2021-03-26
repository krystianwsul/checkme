package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.Notifier
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.core.Single


class DomainUpdater(domainFactory: DomainFactory? = null) {

    private val domainFactorySingle = domainFactory?.let { Single.just(it) } ?: DomainFactory.instanceRelay
            .filterNotNull()
            .firstOrError()

    fun <T : Any> performDomainUpdate(action: (DomainFactory) -> Result<T>): Single<T> {
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

    data class Result<T : Any>(val data: T, val params: Params) {

        constructor(
                data: T,
                notifierParams: Notifier.Params? = null,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(data, Params(notifierParams, notificationType, cloudParams))

        constructor(
                data: T,
                now: ExactTimeStamp.Local,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(data, Params(now, notificationType, cloudParams))
    }

    data class Params(
            val notifierParams: Notifier.Params? = null,
            val notificationType: DomainListenerManager.NotificationType? = null,
            val cloudParams: DomainFactory.CloudParams? = null,
    ) {

        constructor(
                now: ExactTimeStamp.Local,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(Notifier.Params(now), notificationType, cloudParams)
    }
}