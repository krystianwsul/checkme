package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserScope
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.checkme.utils.mapNotNull
import com.mindorks.scheduler.Priority
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable

abstract class DomainListener<DOMAIN_DATA : DomainData> {

    companion object {

        private var dataId = 1

        private val nextId get() = dataId++
    }

    val dataId = DataId(nextId)
    val data = BehaviorRelay.create<DOMAIN_DATA>()!!

    private var disposable: Disposable? = null

    protected abstract val domainResultFetcher: DomainResultFetcher<DOMAIN_DATA>

    protected open val priority: Priority? = null

    fun start(forced: Boolean = false) {
        if (disposable != null) {
            if (forced)
                stop()
            else
                return
        }

        var listenerAdded = false

        disposable = UserScope.instanceRelay
            .observeOnDomain(priority)
            .filterNotNull()
            .switchMap { userScope ->
                /**
                 * What's expected here: the DomainListener may be initialized before the DomainFactory is
                 * available.  And, it can stick around after the DomainFactory is destroyed, such as on logout.
                 * But after logout, all DomainListeners should be destroyed, so we should never see more than
                 * one event get this far.
                 */
                check(!listenerAdded)

                listenerAdded = true

                userScope.domainListenerManager
                    .addListener(this)
                    .map { userScope }
            }
            .toFlowable(BackpressureStrategy.LATEST)
            .observeOnDomain(priority)
            .flatMapMaybe(
                { domainResultFetcher.getDomainResult(it).mapNotNull { it.data } },
                false,
                1,
            )
            .filter { data.value != it }
            .doFinally {
                if (listenerAdded) {
                    UserScope.nullableInstance
                        ?.domainListenerManager
                        ?.removeListener(this)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(data)
    }

    fun stop() {
        disposable?.dispose()
        disposable = null
    }

    interface DomainResultFetcher<DOMAIN_DATA : DomainData> {

        fun getDomainResult(userScope: UserScope): Single<DomainResult<DOMAIN_DATA>>

        open class DomainFactoryDomainResult<DOMAIN_DATA : DomainData>(
            private val getDomainResult: (domainFactory: DomainFactory) -> DomainResult<DOMAIN_DATA>
        ) : DomainResultFetcher<DOMAIN_DATA> {

            override fun getDomainResult(userScope: UserScope): Single<DomainResult<DOMAIN_DATA>> {
                return userScope.domainFactorySingle
                    .map { it as DomainFactory }
                    .map(getDomainResult)
            }
        }

        class DomainFactoryData<DOMAIN_DATA : DomainData>(
            private val getDomainData: (domainFactory: DomainFactory) -> DOMAIN_DATA
        ) : DomainFactoryDomainResult<DOMAIN_DATA>({ DomainResult.Completed(getDomainData(it)) })
    }
}