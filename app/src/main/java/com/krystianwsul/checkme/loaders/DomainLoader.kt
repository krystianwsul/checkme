package com.krystianwsul.checkme.loaders

import android.content.Context
import android.support.v4.content.AsyncTaskLoader

import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ObserverHolder
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.persistencemodel.SaveService

import junit.framework.Assert

abstract class DomainLoader<D : DomainLoader.Data>(context: Context, private val firebaseLevel: FirebaseLevel) : AsyncTaskLoader<D>(context) {

    private var data: D? = null

    private var observer: Observer? = null

    private val domainFactory = DomainFactory.getDomainFactory()

    private val firebaseListener = { domainFactory: DomainFactory ->
        Assert.assertTrue(domainFactory.isConnected)

        if (isStarted)
            forceLoad()
    }

    abstract val name: String

    override fun loadInBackground(): D? {
        if (firebaseLevel == FirebaseLevel.NEED && !domainFactory.isConnected)
            return null

        return if (firebaseLevel == FirebaseLevel.FRIEND && !(domainFactory.isConnected && domainFactory.hasFriends())) null else loadDomain(domainFactory)
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
            ObserverHolder.getObserverHolder().addDomainObserver(observer!!)
        }

        if (takeContentChanged() || data == null) {
            when (firebaseLevel) {
                DomainLoader.FirebaseLevel.NOTHING -> forceLoad()
                DomainLoader.FirebaseLevel.WANT -> {
                    forceLoad()

                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null && !domainFactory.isConnected) {
                        val userInfo = UserInfo(firebaseUser)

                        domainFactory.setUserInfo(context.applicationContext, SaveService.Source.GUI, userInfo)
                    }
                }
                DomainLoader.FirebaseLevel.NEED -> {
                    if (domainFactory.isConnected) {
                        forceLoad()
                    } else {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return

                        val userInfo = UserInfo(firebaseUser)

                        domainFactory.setUserInfo(context.applicationContext, SaveService.Source.GUI, userInfo)
                        domainFactory.addFirebaseListener(firebaseListener)
                    }
                }
                DomainLoader.FirebaseLevel.FRIEND -> {
                    if (domainFactory.isConnected && domainFactory.hasFriends()) {
                        forceLoad()
                    } else {
                        FirebaseAuth.getInstance().currentUser?.let {
                            val userInfo = UserInfo(it)

                            domainFactory.setUserInfo(context.applicationContext, SaveService.Source.GUI, userInfo)
                            domainFactory.addFriendFirebaseListener(firebaseListener)

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

    inner class Observer {
        fun onDomainChanged(dataIds: List<Int>) {
            if (data?.let { dataIds.contains(it.dataId) } == true)
                return

            onContentChanged()
        }
    }

    abstract class Data {

        companion object {

            private var sDataId = 1

            private val nextId get() = sDataId++
        }

        val dataId: Int

        init {
            dataId = nextId
        }
    }

    enum class FirebaseLevel {
        NOTHING,
        WANT,
        NEED,
        FRIEND
    }
}
