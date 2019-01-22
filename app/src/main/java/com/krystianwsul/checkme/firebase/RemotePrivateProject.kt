package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.records.RemotePrivateProjectRecord
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

class RemotePrivateProject(
        domainFactory: DomainFactory,
        remoteProjectRecord: RemotePrivateProjectRecord,
        uuid: String,
        now: ExactTimeStamp) : RemoteProject(domainFactory, remoteProjectRecord, uuid, now) {

    override fun updateRecordOf(addedFriends: Set<RemoteRootUser>, removedFriends: Set<String>) = throw UnsupportedOperationException()
}