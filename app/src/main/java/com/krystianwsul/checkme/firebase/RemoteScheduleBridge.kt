package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.schedules.ScheduleBridge
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord
import com.krystianwsul.common.utils.RemoteCustomTimeId


abstract class RemoteScheduleBridge<T : RemoteCustomTimeId>(
        private val domainFactory: DomainFactory,
        private val remoteScheduleRecord: RemoteScheduleRecord<T>) : ScheduleBridge {

    // use project record instead
    override val customTimeKey by lazy { remoteScheduleRecord.run { customTimeId?.let { domainFactory.getCustomTimeKey(projectId, it) } } }
}