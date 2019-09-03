package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.firebase.PrivateCustomTimeJson
import com.krystianwsul.checkme.utils.RemoteCustomTimeId


class RemotePrivateCustomTimeRecord : RemoteCustomTimeRecord<RemoteCustomTimeId.Private> {

    override val id: RemoteCustomTimeId.Private
    override val remoteProjectRecord: RemotePrivateProjectRecord
    override val customTimeJson: com.krystianwsul.common.firebase.PrivateCustomTimeJson

    constructor(id: RemoteCustomTimeId.Private, remoteProjectRecord: RemotePrivateProjectRecord, customTimeJson: com.krystianwsul.common.firebase.PrivateCustomTimeJson) : super(false) {
        this.id = id
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    constructor(remoteProjectRecord: RemotePrivateProjectRecord, customTimeJson: com.krystianwsul.common.firebase.PrivateCustomTimeJson) : super(true) {
        id = remoteProjectRecord.getCustomTimeRecordId()
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(remoteProjectRecord.remoteCustomTimeRecords.remove(id) == this)

    override fun mine(domainFactory: DomainFactory) = true

    var current: Boolean
        get() = customTimeJson.current
        set(value) {
            if (customTimeJson.current == value)
                return

            customTimeJson.current = value
            addValue("$key/current", value)
        }
}
