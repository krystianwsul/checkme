package com.krystianwsul.checkme.domainmodel.update

import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.subjects.SingleSubject


object AndroidDomainUpdater : DomainUpdater() {

    private lateinit var queue: Queue

    fun init() {
        val isReady = DomainFactory.instanceRelay.switchMap { domainFactoryWrapper ->
            if (domainFactoryWrapper.value == null) {
                Observable.just(NullableWrapper())
            } else {
                domainFactoryWrapper.value
                        .isSaved
                        .map { isSaved ->
                            if (isSaved) {
                                NullableWrapper()
                            } else {
                                NullableWrapper(domainFactoryWrapper.value)
                            }
                        }
            }
        }

        Queue(isReady).subscribe()
    }

    override fun <T : Any> performDomainUpdate(action: (DomainFactory, ExactTimeStamp.Local) -> Result<T>): Single<T> =
            queue.add(action)

    class Queue(private val isReady: Observable<NullableWrapper<DomainFactory>>) {

        // todo queue neither a relay of a list, nor @Synchronized make any sense for what I need here.

        private val itemsRelay = BehaviorRelay.createDefault(listOf<Item>())

        fun subscribe(): Disposable {
            return Observables.combineLatest(itemsRelay, isReady)
                    .observeOnDomain()
                    .subscribe { (items, domainFactoryWrapper) -> dispatchItems(items, domainFactoryWrapper.value) }
        }

        @Synchronized
        fun <T : Any> add(action: (DomainFactory, ExactTimeStamp.Local) -> Result<T>): Single<T> {
            val subject = SingleSubject.create<T>()

            val item = object : Item {

                private lateinit var result: Result<T>

                override fun getParams(domainFactory: DomainFactory, now: ExactTimeStamp.Local): Params {
                    result = action(domainFactory, now)

                    return result.params
                }

                override fun dispatchResult() = subject.onSuccess(result.data)
            }

            itemsRelay.accept(itemsRelay.value + item)

            return subject
        }

        private fun dispatchItems(items: List<Item>, domainFactory: DomainFactory?) {
            DomainThreadChecker.instance.requireDomainThread()

            if (domainFactory == null || items.isEmpty()) return

            val now = ExactTimeStamp.Local.now

            val params = Params.merge(items.map { it.getParams(domainFactory, now) })

            domainFactory.apply {
                params.notifierParams
                        ?.fix(now)
                        ?.let(notifier::updateNotifications)

                params.notificationType?.let(::save)

                params.cloudParams?.let(::notifyCloud)
            }

            items.forEach { it.dispatchResult() }

            itemsRelay.accept(listOf())
        }

        private interface Item {

            fun getParams(domainFactory: DomainFactory, now: ExactTimeStamp.Local): Params
            fun dispatchResult()
        }
    }
}