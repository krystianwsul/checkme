package com.krystianwsul.checkme.domainmodel

import io.reactivex.rxjava3.core.Single


class DomainUpdater(domainFactory: DomainFactory? = null) {

    private val domainFactorySingle = domainFactory?.let { Single.just(it) } ?: DomainFactory.onReady()

    fun <T : Any> updateDomainSingle(action: DomainFactory.() -> Result<T>): Single<T> {
        return domainFactorySingle.doOnSuccess { check(!it.isSaved.value!!) }
                .map { it to it.action() }
                .doOnSuccess { (domainFactory, result) ->
                    result.params
                            .cloudParams
                            ?.let(domainFactory::notifyCloud)
                }
                .map { it.second.data }
    }

    fun updateDomainCompletable(action: DomainFactory.() -> Params) =
            updateDomainSingle { Result(Unit, action()) }.ignoreElement()!!

    data class Result<T : Any>(val data: T, val params: Params) {

        constructor(data: T, cloudParams: DomainFactory.CloudParams) : this(data, Params(cloudParams))
    }

    data class Params(val cloudParams: DomainFactory.CloudParams? = null)
}