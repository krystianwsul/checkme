package com.krystianwsul.checkme.domainmodel.update

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.utils.filterNotNull
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

        private val items = mutableListOf<Item>()

        private val trigger = PublishRelay.create<Unit>()

        fun subscribe(): Disposable {
            return Observables.combineLatest(trigger, isReady)
                    .map { (_, domainFactoryWrapper) -> domainFactoryWrapper }
                    .filterNotNull()
                    .map {
                        it to synchronized(items) {
                            items.toMutableList().also { items -= it }
                        }
                    }
                    .filter { (_, items) -> items.isNotEmpty() }
                    .observeOnDomain()
                    .subscribe { (domainFactory, items) -> dispatchItems(domainFactory, items) }
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

            synchronized(items) { items += item }

            trigger.accept(Unit)

            return subject
        }

        private fun dispatchItems(domainFactory: DomainFactory, items: List<Item>) {
            DomainThreadChecker.instance.requireDomainThread()

            check(items.isNotEmpty())

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
        }

        private interface Item {

            fun getParams(domainFactory: DomainFactory, now: ExactTimeStamp.Local): Params
            fun dispatchResult()
        }
    }
}