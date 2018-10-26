package com.krystianwsul.checkme.domainmodel

import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.RemoteProjectFactory
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

class KotlinDomainFactory {

    companion object {

        var _kotlinDomainFactory: KotlinDomainFactory? = null

        @Synchronized
        fun getKotlinDomainFactory(): KotlinDomainFactory {
            if (_kotlinDomainFactory == null)
                _kotlinDomainFactory = KotlinDomainFactory()
            return _kotlinDomainFactory!!
        }
    }

    val domainFactory: DomainFactory

    private var start: ExactTimeStamp
    private var read: ExactTimeStamp
    private var stop: ExactTimeStamp

    val readMillis get() = read.long - start.long
    val instantiateMillis get() = stop.long - read.long

    var userInfo: UserInfo? = null

    var recordQuery: Query? = null
    var recordListener: ValueEventListener? = null

    var userQuery: Query? = null
    var userListener: ValueEventListener? = null

    lateinit var localFactory: LocalFactory

    var remoteProjectFactory: RemoteProjectFactory? = null

    init {
        start = ExactTimeStamp.now

        domainFactory = DomainFactory(this)

        read = ExactTimeStamp.now

        domainFactory.initialize()

        stop = ExactTimeStamp.now
    }
}