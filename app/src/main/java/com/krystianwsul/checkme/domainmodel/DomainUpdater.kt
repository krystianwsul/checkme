package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.core.Single


class DomainUpdater(domainFactory: DomainFactory? = null) {

    private val domainFactorySingle = domainFactory?.onReady() ?: DomainFactory.onReady()

    fun <T : Any> updateDomainSingle(action: DomainFactory.() -> Result<T>): Single<T> {
        return domainFactorySingle.doOnSuccess { check(!it.isSaved.value!!) }
                .map { it to it.action() }
                .doOnSuccess { (domainFactory, result) ->
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
                .map { it.second.data }
    }

    fun updateDomainCompletable(action: DomainFactory.() -> Params) =
            updateDomainSingle { Result(Unit, action()) }.ignoreElement()!!

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