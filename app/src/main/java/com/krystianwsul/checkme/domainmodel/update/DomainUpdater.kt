package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.Notifier
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.core.Single


abstract class DomainUpdater {

    protected abstract fun <T : Any> performDomainUpdate(action: (DomainFactory) -> Result<T>): Single<T>

    fun <T : Any> updateDomainSingle(singleDomainUpdate: SingleDomainUpdate<T>): Single<T> =
            performDomainUpdate(singleDomainUpdate.action)

    fun updateDomainCompletable(completableDomainUpdate: CompletableDomainUpdate) =
            performDomainUpdate { Result(Unit, completableDomainUpdate.action(it)) }.ignoreElement()!!

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