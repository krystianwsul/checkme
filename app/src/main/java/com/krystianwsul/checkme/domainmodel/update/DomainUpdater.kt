package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.Notifier
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.core.Single


abstract class DomainUpdater {

    protected abstract fun <T : Any> performDomainUpdate(
            action: (DomainFactory, ExactTimeStamp.Local) -> Result<T>,
    ): Single<T>

    fun <T : Any> updateDomainSingle(singleDomainUpdate: SingleDomainUpdate<T>): Single<T> =
            performDomainUpdate(singleDomainUpdate.action)

    fun updateDomainCompletable(completableDomainUpdate: CompletableDomainUpdate) =
            performDomainUpdate { domainFactory, now ->
                Result(Unit, completableDomainUpdate.action(domainFactory, now))
            }.ignoreElement()!!

    data class Result<T : Any>(val data: T, val params: Params) {

        constructor(
                data: T,
                notifierParams: Notifier.Params? = null,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(data, Params(notifierParams, notificationType, cloudParams))

        constructor(
                data: T,
                notify: Boolean,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(data, Params(if (notify) Notifier.Params() else null, notificationType, cloudParams))
    }

    data class Params(
            val notifierParams: Notifier.Params? = null,
            val notificationType: DomainListenerManager.NotificationType? = null,
            val cloudParams: DomainFactory.CloudParams? = null,
    ) {

        companion object {

            fun merge(params: List<Params>): Params {
                val notifierParams = Notifier.Params.merge(params.mapNotNull { it.notifierParams })

                val notificationType =
                        DomainListenerManager.NotificationType.merge(params.mapNotNull { it.notificationType })

                val cloudParams = params.mapNotNull { it.cloudParams }
                        .takeIf { it.isNotEmpty() }
                        ?.let { DomainFactory.CloudParams(it.flatMap { it.projects }, it.flatMap { it.userKeys }) }

                return Params(notifierParams, notificationType, cloudParams)
            }
        }

        constructor(
                notify: Boolean,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(if (notify) Notifier.Params() else null, notificationType, cloudParams)
    }
}