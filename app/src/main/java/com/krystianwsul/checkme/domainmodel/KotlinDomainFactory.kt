package com.krystianwsul.checkme.domainmodel

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

    init {
        domainFactory.initialize()

        stop = ExactTimeStamp.now
    }

    val readMillis get() = read.long - start.long

    val instantiateMillis get() = stop.long - read.long

    var userInfo: UserInfo? = null
}