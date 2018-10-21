package com.krystianwsul.checkme.loaders

import android.content.Context
import android.support.v4.content.AsyncTaskLoader
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ObserverHolder
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.RemoteFriendFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.DomainObserver

abstract class DomainLoader<D : DomainData>(context: Context, private val firebaseLevel: FirebaseLevel) : AsyncTaskLoader<D>(context) {

    private var data: D? = null

    private var observer: Observer? = null

    private val domainFactory = DomainFactory.getDomainFactory()

    private val firebaseListener = { domainFactory: DomainFactory ->
        check(domainFactory.isConnected)

        if (isStarted)
            forceLoad()
    }

    abstract val name: String

    override fun loadInBackground(): D? {
        if (firebaseLevel == FirebaseLevel.NEED && !domainFactory.isConnected)
            return null

        return if (firebaseLevel == FirebaseLevel.FRIEND && !(domainFactory.isConnected && RemoteFriendFactory.hasFriends())) null else loadDomain(domainFactory)
    }

    protected abstract fun loadDomain(domainFactory: DomainFactory): D

    // main thread
    override fun deliverResult(data: D?) {
        if (data == null)
            return

        if (isReset)
            return

        if (this.data == null || this.data != data) {
            this.data = data

            if (isStarted)
                super.deliverResult(data)
        }
    }

    // main thread
    override fun onStartLoading() {
        if (data != null)
            deliverResult(data)

        if (observer == null) {
            observer = Observer()
            ObserverHolder.addDomainObserver(observer!!)
        }

        if (takeContentChanged() || data == null) {
            when (firebaseLevel) {
                FirebaseLevel.NOTHING -> forceLoad()
                FirebaseLevel.WANT -> {
                    forceLoad()

                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null && !domainFactory.isConnected) {
                        val userInfo = UserInfo(firebaseUser)

                        domainFactory.setUserInfo(context.applicationContext, SaveService.Source.GUI, userInfo)
                    }
                }
                FirebaseLevel.NEED -> {
                    if (domainFactory.isConnected) {
                        forceLoad()
                    } else {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return

                        val userInfo = UserInfo(firebaseUser)

                        domainFactory.setUserInfo(context.applicationContext, SaveService.Source.GUI, userInfo)
                        domainFactory.addFirebaseListener(firebaseListener)
                    }
                }
                FirebaseLevel.FRIEND -> {
                    if (domainFactory.isConnected && RemoteFriendFactory.hasFriends()) {
                        forceLoad()
                    } else {
                        FirebaseAuth.getInstance().currentUser?.let {
                            val userInfo = UserInfo(it)

                            domainFactory.setUserInfo(context.applicationContext, SaveService.Source.GUI, userInfo)
                            RemoteFriendFactory.addFriendListener { firebaseListener(DomainFactory.getDomainFactory()) }
                        }
                    }
                }
            }
        }
    }

    override fun onStopLoading() {
        domainFactory.removeFirebaseListener(firebaseListener)

        cancelLoad()
    }

    override fun onReset() {
        onStopLoading()

        data = null
        observer = null
    }

    inner class Observer : DomainObserver {

        override fun onDomainChanged(dataIds: List<Int>) {
            if (data?.let { dataIds.contains(it.dataId) } == true)
                return

            onContentChanged()
        }
    }
}
