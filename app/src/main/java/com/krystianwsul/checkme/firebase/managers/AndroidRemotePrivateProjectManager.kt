package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.time.ExactTimeStamp

class AndroidRemotePrivateProjectManager(
        private val domainFactory: DomainFactory,
        dataSnapshot: DataSnapshot,
        now: ExactTimeStamp
) : RemotePrivateProjectManager(domainFactory, dataSnapshot, now)