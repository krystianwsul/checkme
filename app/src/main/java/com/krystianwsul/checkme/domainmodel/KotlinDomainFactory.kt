package com.krystianwsul.checkme.domainmodel

import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
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

    private var start = ExactTimeStamp.now
    val domainFactory = DomainFactory(this)
    private var read: ExactTimeStamp = ExactTimeStamp.now
    private var stop: ExactTimeStamp

    val readMillis get() = read.long - start.long

    val instantiateMillis get() = stop.long - read.long

    var userInfo: UserInfo? = null

    var recordQuery: Query? = null

    var recordListener: ValueEventListener? = null

    var userQuery: Query? = null

    var userListener: ValueEventListener? = null

    init {
        domainFactory.initialize()

        stop = ExactTimeStamp.now
    }
}