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
                notifierParams: NotifierParams? = null,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(data, Params(notifierParams, notificationType, cloudParams))

        constructor(
                data: T,
                notify: Boolean,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(data, Params(if (notify) NotifierParams() else null, notificationType, cloudParams))
    }

    data class Params(
            val notifierParams: NotifierParams? = null,
            val notificationType: DomainListenerManager.NotificationType? = null,
            val cloudParams: DomainFactory.CloudParams? = null,
    ) {

        constructor(
                notify: Boolean,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(if (notify) NotifierParams() else null, notificationType, cloudParams)
    }

    data class NotifierParams(
            val sourceName: String = "other",
            val silent: Boolean = true,
            val clear: Boolean = false,
    ) {

        fun fix(now: ExactTimeStamp.Local) = Notifier.Params(now, sourceName, silent, clear)
    }
}