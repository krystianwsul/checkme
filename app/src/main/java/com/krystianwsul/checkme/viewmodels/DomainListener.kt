package com.krystianwsul.checkme.viewmodels

import android.util.Log
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserScope
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.mindorks.scheduler.Priority
import com.mindorks.scheduler.internal.CustomPriorityScheduler
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
    val data = BehaviorRelay.create<DOMAIN_DATA>()

    private var disposable: Disposable? = null

    protected abstract val domainResultFetcher: DomainResultFetcher<DOMAIN_DATA>

    protected open val priority: Priority? = null // todo scheduling

    open fun start(forced: Boolean = false) { // todo scheduling
        if (disposable != null) {
            if (forced)
                stop()
            else
                return
        }

        var listenerAdded = false

        Log.e("asdf", "magic domainListener start") // todo scheduling
        disposable = UserScope.instanceRelay
            .doOnNext {
                Log.e("asdf", "magic domainListener 0 null? " + (it.value == null)) // todo scheduling
            }
            /*
            todo scheduling: In the situation that the DomainListener is started before the DomainFactory is available,
            this does guarantee that it emits before RootTasks get read.  However, the subsequent events' priority comes
            from the AndroidDomainUpdater, which gets run via DomainFactory.onChangeTypeEvent.  So I think that it would
            suffice to just use a high priority for this initial observeOnDomain, and figure out that second priority
            as a separate issue.

            Also, overriding priority for EditActivity may be unnecessary at that point, since that was a "first read" issue
             */
            //.observeOnDomain(priority)
            .observeOnDomain(Priority.FIRST_READ)
            .doOnNext {
                Log.e(
                    "asdf",
                    "magic domainListener 1, priority " + CustomPriorityScheduler.currentPriority.get()
                ) // todo scheduling
                DomainThreadChecker.instance.requireDomainThread()
            }
            .doOnNext {
                Log.e("asdf", "magic domainListener 1a null? " + (it.value == null)) // todo scheduling
            }
            .filterNotNull()
            .switchMap { userScope ->
                Log.e("asdf", "magic domainListener 2a") // todo scheduling

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
                    .doOnNext {
                        Log.e("asdf", "magic domainListener 2b") // todo scheduling
                    }
            }
            .doOnNext {
                Log.e("asdf", "magic domainListener 2c") // todo scheduling
            }
            .toFlowable(BackpressureStrategy.LATEST)
            .flatMapMaybe(
                {
                    Log.e(
                        "asdf",
                        "magic domainListener 2, priority " + CustomPriorityScheduler.currentPriority.get()
                    ) // todo scheduling
                    DomainThreadChecker.instance.requireDomainThread()

                    domainResultFetcher.getDomainResult(it).doOnSuccess {
                        Log.e(
                            "asdf",
                            "magic domainListener 3, priority " + CustomPriorityScheduler.currentPriority.get()
                        ) // todo scheduling
                    }.mapNotNull { it.data }
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