package com.krystianwsul.checkme.domainmodel.update

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.gui.main.DebugFragment
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.time.ExactTimeStamp
import com.mindorks.scheduler.Priority
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.subjects.SingleSubject


object AndroidDomainUpdater : DomainUpdater() {

    private lateinit var queue: Queue

    fun init() {
        // todo I think this is redundant with the second observeOnDomain inside queue, but whatever
        val isReady = DomainFactory.instanceRelay.observeOnDomain(Priority.FIRST_READ)

        queue = Queue(isReady).apply { subscribe() }
    }

    override fun <T : Any> performDomainUpdate(domainUpdate: DomainUpdate<T>): Single<T> = queue.add(domainUpdate)

    class Queue(private val isReady: Observable<NullableWrapper<DomainFactory>>) {

        private val items = mutableListOf<Item>()

        private val triggerRelay = PublishRelay.create<Boolean>()

        fun subscribe(): Disposable {
            return Observables.combineLatest(triggerRelay, isReady)
                .flatMapSingle { (highPriority, domainFactoryWrapper) ->
                    Single.just(domainFactoryWrapper).observeOnDomain(Priority.FIRST_READ.takeIf { highPriority })
                }
                .filterNotNull()
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

            triggerRelay.accept(domainUpdate.highPriority)

            return subject
        }

        private fun dispatchItems(domainFactory: DomainFactory, items: List<Item>) {
            MyCrashlytics.log("AndroidDomainUpdater.dispatchItems begin: " + items.joinToString(", ") { it.name })
            DebugFragment.logDone("AndroidDomainUpdater.dispatchItems start")

            DomainThreadChecker.instance.requireDomainThread()

            check(items.isNotEmpty())

            val now = ExactTimeStamp.Local.now

            val params = Params.merge(items.map {
                MyCrashlytics.log("AndroidDomainUpdater.dispatchItems getParams " + it.name)
                it.getParams(domainFactory, now)
            })

            items.forEach {
                MyCrashlytics.log("AndroidDomainUpdater.dispatchItems dispatchResult " + it.name)
                it.dispatchResult()
            }

            onUpdated.accept(Unit)

            DebugFragment.logDone("AndroidDomainUpdater.dispatchItems updating notifications")
            domainFactory.updateNotifications(params, now)
            DebugFragment.logDone("AndroidDomainUpdater.dispatchItems notifications updated")

            /*
            domainFactory.projectsFactory.apply {
                checkInconsistentRootTaskIds(allDependenciesLoadedTasks.filterIsInstance<RootTask>(), projects.values)
            }
             */

            domainFactory.saveAndNotifyCloud(params, now)

            MyCrashlytics.log("AndroidDomainUpdater.dispatchItems end")
            DebugFragment.logDone("AndroidDomainUpdater.dispatchItems end")
        }

        private interface Item {

            val name: String

            fun getParams(domainFactory: DomainFactory, now: ExactTimeStamp.Local): Params
            fun dispatchResult()
        }
    }
}