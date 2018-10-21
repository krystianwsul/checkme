package com.krystianwsul.checkme.viewmodels

import android.arch.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ObserverHolder
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.RemoteFriendFactory
import com.krystianwsul.checkme.loaders.DomainData
import com.krystianwsul.checkme.loaders.FirebaseLevel
import com.krystianwsul.checkme.persistencemodel.SaveService
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers

abstract class DomainViewModel<D : DomainData> : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    val data = BehaviorRelay.create<NullableWrapper<D>>()!!

    private var observer: Observer? = null

    private val domainFactory = DomainFactory.getDomainFactory()

    private val firebaseListener = { domainFactory: DomainFactory ->
        check(domainFactory.isConnected)

        load()
    }

    private lateinit var firebaseLevel: FirebaseLevel

    protected fun start(firebaseLevel: FirebaseLevel) {
        this.firebaseLevel = firebaseLevel

        if (observer == null) {
            observer = Observer()
            ObserverHolder.addDomainObserver(observer!!)
        }

        when (firebaseLevel) {
            FirebaseLevel.NOTHING -> load()
            FirebaseLevel.WANT -> {
                load()

                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null && !domainFactory.isConnected) {
                    val userInfo = UserInfo(firebaseUser)

                    domainFactory.setUserInfo(MyApplication.instance, SaveService.Source.GUI, userInfo)
                    domainFactory.addFirebaseListener(firebaseListener)
                }
            }
            FirebaseLevel.NEED -> {
                if (domainFactory.isConnected) {
                    load()
                } else {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return

                    val userInfo = UserInfo(firebaseUser)

                    domainFactory.setUserInfo(MyApplication.instance, SaveService.Source.GUI, userInfo)
                    domainFactory.addFirebaseListener(firebaseListener)
                }
            }
            FirebaseLevel.FRIEND -> {
                if (domainFactory.isConnected && RemoteFriendFactory.hasFriends()) {
                    load()
                } else {
                    FirebaseAuth.getInstance().currentUser?.let {
                        val userInfo = UserInfo(it)

                        domainFactory.setUserInfo(MyApplication.instance, SaveService.Source.GUI, userInfo)
                        RemoteFriendFactory.addFriendListener { firebaseListener(domainFactory) }
                    }
                }
            }
        }
    }

    fun stop() {
        domainFactory.removeFirebaseListener(firebaseListener)
        observer = null
        compositeDisposable.clear()
    }

    private fun load() {
        if (firebaseLevel == FirebaseLevel.NEED && !domainFactory.isConnected)
            return

        if (firebaseLevel == FirebaseLevel.FRIEND && !(domainFactory.isConnected && RemoteFriendFactory.hasFriends()))
            return

        Single.fromCallable { NullableWrapper(getData(domainFactory)) }
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { loaded ->
                    if (data.value != loaded)
                        data.accept(loaded)
                }
                .addTo(compositeDisposable)
    }

    protected abstract fun getData(domainFactory: DomainFactory): D?

    override fun onCleared() = stop()

    inner class Observer : DomainObserver {

        override fun onDomainChanged(dataIds: List<Int>) {
            if (data.value?.value?.let { dataIds.contains(it.dataId) } == true)
                return

            load()
        }
    }
}