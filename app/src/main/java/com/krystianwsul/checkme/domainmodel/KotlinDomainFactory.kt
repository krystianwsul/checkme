package com.krystianwsul.checkme.domainmodel

import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.firebase.RemoteProjectFactory
import com.krystianwsul.checkme.firebase.RemoteRootUser
import com.krystianwsul.checkme.persistencemodel.PersistenceManger
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

class KotlinDomainFactory(persistenceManager: PersistenceManger?) {

    companion object {

        var _kotlinDomainFactory: KotlinDomainFactory? = null

        @Synchronized
        fun getKotlinDomainFactory(persistenceManager: PersistenceManger? = null): KotlinDomainFactory {
            if (_kotlinDomainFactory == null)
                _kotlinDomainFactory = KotlinDomainFactory(persistenceManager)
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

    @JvmField
    var localFactory: LocalFactory

    var remoteProjectFactory: RemoteProjectFactory? = null

    var remoteRootUser: RemoteRootUser? = null

    val notTickFirebaseListeners = mutableListOf<(DomainFactory) -> Unit>()

    var tickData: TickData? = null

    var skipSave = false

    val lastNotificationBeeps = mutableMapOf<InstanceKey, Long>()

    init {
        start = ExactTimeStamp.now

        domainFactory = DomainFactory(this)
        localFactory = persistenceManager?.let { LocalFactory(it) } ?: LocalFactory.instance

        read = ExactTimeStamp.now

        localFactory.initialize(domainFactory)

        stop = ExactTimeStamp.now
    }
}