package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord

abstract class RemoteScheduleBridge(
        private val domainFactory: DomainFactory,
        private val remoteScheduleRecord: RemoteScheduleRecord) : ScheduleBridge {

    // use project record instead
    override val customTimeKey by lazy { remoteScheduleRecord.run { customTimeId?.let { domainFactory.getCustomTimeKey(projectId, it) } } }
}