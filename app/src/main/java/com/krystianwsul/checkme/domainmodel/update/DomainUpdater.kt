package com.krystianwsul.checkme.domainmodel.update

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.Notifier
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.core.Single


abstract class DomainUpdater {

    companion object {

        fun DomainFactory.updateNotifications(params: Params, now: ExactTimeStamp.Local) {
            params.notifierParams?.let { notifier.updateNotifications(now, it) }
        }

        fun DomainFactory.saveAndNotifyCloud(params: Params, now: ExactTimeStamp.Local) {
            params.apply {
                saveParams?.let { save(it, now) }

                cloudParams?.let(::notifyCloud)
            }
        }
    }

    abstract fun <T : Any> performDomainUpdate(domainUpdate: DomainUpdate<T>, trigger: Boolean = true): Single<T>

    data class Result<T : Any>(val data: T, val params: Params) {

        constructor(
                data: T,
                notifierParams: Notifier.Params? = null,
                saveParams: DomainFactory.SaveParams? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(data, Params(notifierParams, saveParams, cloudParams))

        constructor(
                data: T,
                notify: Boolean,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(data, Params(notify, notificationType, cloudParams))
    }

    data class Params(
            val notifierParams: Notifier.Params? = null,
            val saveParams: DomainFactory.SaveParams? = null,
            val cloudParams: DomainFactory.CloudParams? = null,
    ) {

        companion object {

            fun merge(params: List<Params>): Params {
                val notifierParams = Notifier.Params.merge(params.mapNotNull { it.notifierParams })

                val saveParams = DomainFactory.SaveParams.merge(params.mapNotNull { it.saveParams })

                val cloudParams = params.mapNotNull { it.cloudParams }
                        .takeIf { it.isNotEmpty() }
                        ?.let { DomainFactory.CloudParams(it.flatMap { it.projects }, it.flatMap { it.userKeys }) }

                return Params(notifierParams, saveParams, cloudParams)
            }
        }

        constructor(
                notify: Boolean,
                notificationType: DomainListenerManager.NotificationType? = null,
                cloudParams: DomainFactory.CloudParams? = null,
        ) : this(
                if (notify) Notifier.Params() else null,
                notificationType?.let(DomainFactory::SaveParams),
                cloudParams,
        )
    }
}