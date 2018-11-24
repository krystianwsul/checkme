package com.krystianwsul.checkme.viewmodels

import android.arch.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ObserverHolder
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.RemoteFriendFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers

abstract class DomainViewModel<D : DomainData> : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    val data = BehaviorRelay.create<D>()

    private var observer: Observer? = null

    protected val kotlinDomainFactory = DomainFactory.getKotlinDomainFactory()

    private val firebaseListener = { _: DomainFactory ->
        check(kotlinDomainFactory.getIsConnected())

        load()
    }

    private lateinit var firebaseLevel: FirebaseLevel

    protected fun internalStart(firebaseLevel: FirebaseLevel) {
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
                if (firebaseUser != null && !kotlinDomainFactory.getIsConnected()) {
                    val userInfo = UserInfo(firebaseUser)

                    kotlinDomainFactory.setUserInfo(SaveService.Source.GUI, userInfo)
                    kotlinDomainFactory.addFirebaseListener(firebaseListener)
                }
            }
            FirebaseLevel.NEED -> {
                if (kotlinDomainFactory.getIsConnected()) {
                    load()
                } else {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
                    val userInfo = UserInfo(firebaseUser)

                    kotlinDomainFactory.setUserInfo(SaveService.Source.GUI, userInfo)
                    kotlinDomainFactory.addFirebaseListener(firebaseListener)
                }
            }
            FirebaseLevel.FRIEND -> {
                if (kotlinDomainFactory.getIsConnected() && RemoteFriendFactory.hasFriends()) {
                    load()
                } else {
                    FirebaseAuth.getInstance().currentUser?.let {
                        val userInfo = UserInfo(it)

                        kotlinDomainFactory.setUserInfo(SaveService.Source.GUI, userInfo)
                        RemoteFriendFactory.addFriendListener { firebaseListener(kotlinDomainFactory) }
                    }
                }
            }
        }
    }

    fun stop() {
        kotlinDomainFactory.removeFirebaseListener(firebaseListener)

        observer?.let { ObserverHolder.removeDomainObserver(it) }
        observer = null

        compositeDisposable.clear()
    }

    private fun load() {
        if (firebaseLevel == FirebaseLevel.NEED && !kotlinDomainFactory.getIsConnected())
            return

        if (firebaseLevel == FirebaseLevel.FRIEND && !(kotlinDomainFactory.getIsConnected() && RemoteFriendFactory.hasFriends()))
            return

        Single.fromCallable { getData() }
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { loaded ->
                    if (data.value != loaded)
                        data.accept(loaded)
                }
                .addTo(compositeDisposable)
    }

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