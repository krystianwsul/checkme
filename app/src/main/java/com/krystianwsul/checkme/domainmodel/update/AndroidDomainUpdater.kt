package com.krystianwsul.checkme.domainmodel.update

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyCrashlytics
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
        val isReady = DomainFactory.instanceRelay.observeOnDomain()

        queue = Queue(isReady).apply { subscribe() }
    }

    override fun <T : Any> performDomainUpdate(domainUpdate: DomainUpdate<T>): Single<T> = queue.add(domainUpdate)

    class Queue(private val isReady: Observable<NullableWrapper<DomainFactory>>) {

        private val items = mutableListOf<Item>()

        private val triggerRelay = PublishRelay.create<Unit>()

        fun subscribe(): Disposable {
            return Observables.combineLatest(triggerRelay, isReady)
                    .map { (_, domainFactoryWrapper) -> domainFactoryWrapper }
                    .filterNotNull()
                    .observeOnDomain()
                    .subscribe { domainFactory ->
                        val currItems = synchronized(items) {
                            items.toMutableList().also { items -= it }
                        }

                        if (currItems.isNotEmpty()) dispatchItems(domainFactory, currItems)
                    }
        }

        fun <T : Any> add(domainUpdate: DomainUpdate<T>): Single<T> {
            MyCrashlytics.log("AndroidDomainUpdater.add ${domainUpdate.name}")

            val subject = SingleSubject.create<T>()

            val item = object : Item {

                private lateinit var result: Result<T>

                override val name = domainUpdate.name

                override fun getParams(domainFactory: DomainFactory, now: ExactTimeStamp.Local): Params {
                    result = domainUpdate.doAction(domainFactory, now)

                    return result.params
                }

                override fun dispatchResult() = subject.onSuccess(result.data)
            }

            synchronized(items) { items += item }

            triggerRelay.accept(Unit)

            return subject
        }

        private fun dispatchItems(domainFactory: DomainFactory, items: List<Item>) {
            MyCrashlytics.log("AndroidDomainUpdater.dispatchItems begin: " + items.joinToString(", ") { it.name })

            DomainThreadChecker.instance.requireDomainThread()

            check(items.isNotEmpty())

            val now = ExactTimeStamp.Local.now

            val params = Params.merge(items.map {
                MyCrashlytics.log("AndroidDomainUpdater.dispatchItems getParams " + it.name)
                it.getParams(domainFactory, now)
            })

            domainFactory.updateNotifications(params, now)

            items.forEach {
                MyCrashlytics.log("AndroidDomainUpdater.dispatchItems dispatchResult " + it.name)
                it.dispatchResult()
            }

            domainFactory.saveAndNotifyCloud(params, now)

            MyCrashlytics.log("AndroidDomainUpdater.dispatchItems end")
        }

        private interface Item {

            val name: String

            fun getParams(domainFactory: DomainFactory, now: ExactTimeStamp.Local): Params
            fun dispatchResult()
        }
    }
}