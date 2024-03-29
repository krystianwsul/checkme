package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserScope
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.database.DomainFactoryInitializationDelayProvider
import com.krystianwsul.checkme.firebase.database.TaskPriorityMapper
import com.krystianwsul.checkme.firebase.database.TaskPriorityMapperQueue
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.mindorks.scheduler.Priority
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable

abstract class DomainListener<DOMAIN_DATA : DomainData> : TaskPriorityMapperQueue.Provider {

    companion object {

        private var dataId = 1

        private val nextId get() = dataId++
    }

    val dataId = DataId(nextId)
    val data = BehaviorRelay.create<DOMAIN_DATA>()

    private var disposable: Disposable? = null

    protected abstract val domainResultFetcher: DomainResultFetcher<DOMAIN_DATA>

    protected open val priority = Priority.FIRST_READ

    override fun newDelayProvider(): DomainFactoryInitializationDelayProvider? = null

    override fun newTaskPriorityMapper(): TaskPriorityMapper? = null

    fun start(forced: Boolean = false) {
        if (disposable != null) {
            if (forced)
                stop()
            else
                return
        }

        TaskPriorityMapperQueue.addProvider(this)

        var listenerAdded = false

        disposable = UserScope.instanceRelay
            .observeOnDomain(priority)
            .doOnNext { DomainThreadChecker.instance.requireDomainThread() }
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
            .flatMapMaybe(
                {
                    DomainThreadChecker.instance.requireDomainThread()

                    domainResultFetcher.getDomainResult(it).mapNotNull { it.data }
                },
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
        TaskPriorityMapperQueue.removeProvider(this)

        disposable?.dispose()
        disposable = null
    }

    interface DomainResultFetcher<DOMAIN_DATA : DomainData> {

        fun getDomainResult(userScope: UserScope): Single<DomainResult<DOMAIN_DATA>>

        open class DomainFactoryDomainResult<DOMAIN_DATA : DomainData>(
            private val newDomainQuery: (DomainFactory) -> DomainQuery<DOMAIN_DATA>,
        ) : DomainResultFetcher<DOMAIN_DATA> {

            override fun getDomainResult(userScope: UserScope): Single<DomainResult<DOMAIN_DATA>> {
                var domainQuery: DomainQuery<DOMAIN_DATA>? = null

                return userScope.domainFactorySingle
                    .map { it as DomainFactory }
                    .map {
                        domainQuery = newDomainQuery(it)

                        domainQuery!!.getDomainResult()
                    }
                    .doOnDispose { domainQuery?.interrupt() }
            }

        }

        class DomainFactoryData<DOMAIN_DATA : DomainData>(
            private val getDomainData: (domainFactory: DomainFactory) -> DOMAIN_DATA,
        ) : DomainFactoryDomainResult<DOMAIN_DATA>({
            object : DomainQuery<DOMAIN_DATA> {

                override fun getDomainResult() = DomainResult.Completed(getDomainData(it))
            }
        })
    }
}