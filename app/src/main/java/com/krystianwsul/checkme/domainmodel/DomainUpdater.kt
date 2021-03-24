package com.krystianwsul.checkme.domainmodel

import io.reactivex.rxjava3.core.Single


class DomainUpdater(domainFactory: DomainFactory? = null) {

    private val domainFactorySingle = domainFactory?.let { Single.just(it) } ?: DomainFactory.onReady()

    fun <T : Any> updateDomainSingle(action: DomainFactory.() -> Result<T>): Single<T> {
        return domainFactorySingle.doOnSuccess { check(!it.isSaved.value!!) }
                .map { it to it.action() }
                .doOnSuccess { (domainFactory, result) ->
                    domainFactory.apply {
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
                notificationType: DomainListenerManager.NotificationType?,
                cloudParams: DomainFactory.CloudParams,
        ) : this(data, Params(notificationType, cloudParams))
    }

    data class Params(
            val notificationType: DomainListenerManager.NotificationType? = null,
            val cloudParams: DomainFactory.CloudParams? = null,
    )
}