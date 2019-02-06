package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.checkme.firebase.json.PrivateProjectJson

class RemotePrivateProjectRecord(
        create: Boolean,
        domainFactory: DomainFactory,
        id: String,
        override val projectJson: PrivateProjectJson) : RemoteProjectRecord(create, domainFactory, id) {

    override val remoteCustomTimeRecords = projectJson.customTimes
            .mapValues { (id, customTimeJson) ->
                check(!TextUtils.isEmpty(id))

                RemotePrivateCustomTimeRecord(id, this, customTimeJson)
            }
            .toMutableMap()

    constructor(domainFactory: DomainFactory, id: String, projectJson: PrivateProjectJson) : this(
            false,
            domainFactory,
            id,
            projectJson)

    constructor(domainFactory: DomainFactory, userInfo: UserInfo, projectJson: PrivateProjectJson) : this(
            true,
            domainFactory,
            userInfo.key,
            projectJson)

    fun newRemoteCustomTimeRecord(customTimeJson: PrivateCustomTimeJson): RemotePrivateCustomTimeRecord {
        val remoteCustomTimeRecord = RemotePrivateCustomTimeRecord(this, customTimeJson)
        check(!remoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.id))

        remoteCustomTimeRecords[remoteCustomTimeRecord.id] = remoteCustomTimeRecord
        return remoteCustomTimeRecord
    }

    private val createProjectJson
        get() = projectJson.apply {
            tasks = remoteTaskRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            taskHierarchies = remoteTaskHierarchyRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            customTimes = remoteCustomTimeRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            users = remoteUserRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()
        }

    override val createObject get() = createProjectJson

    override val childKey get() = key

    override fun deleteFromParent() = throw UnsupportedOperationException()
}