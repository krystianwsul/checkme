package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ObserverHolder
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers

abstract class DomainViewModel<D : DomainData> : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    val data = BehaviorRelay.create<D>()

    private var observer: Observer? = null

    protected val domainFactory = DomainFactory.instance

    private val firebaseListener: (DomainFactory) -> Unit = { load() }

    protected fun internalStart() {
        if (observer == null) {
            observer = Observer()
            ObserverHolder.addDomainObserver(observer!!)
        }

        DomainFactory.addFirebaseListener(firebaseListener)
    }

    fun stop() {
        DomainFactory.removeFirebaseListener(firebaseListener)

        observer?.let { ObserverHolder.removeDomainObserver(it) }
        observer = null

        compositeDisposable.clear()
    }

    private fun load() = Single.fromCallable { getData() }
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { loaded ->
                    if (data.value != loaded)
                        data.accept(loaded)
                }
                .addTo(compositeDisposable)

    protected abstract fun getData(): D

    override fun onCleared() = stop()

    inner class Observer : DomainObserver {

        override fun onDomainChanged(dataIds: List<Int>) {
            if (data.value?.let { dataIds.contains(it.dataId) } == true)
                return

            load()
        }
    }
}