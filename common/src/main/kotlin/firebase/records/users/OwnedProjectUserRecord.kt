package com.krystianwsul.common.firebase.records.users

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.users.UserJson
import com.krystianwsul.common.firebase.records.project.SharedOwnedProjectRecord


class OwnedProjectUserRecord(
    create: Boolean,
    private val remoteProjectRecord: SharedOwnedProjectRecord,
    createObject: UserJson
) : ProjectUserRecord(create, remoteProjectRecord, createObject) {

    override var name by Committer(createObject::name)

    override var photoUrl by Committer(createObject::photoUrl)

    fun setToken(deviceDbInfo: DeviceDbInfo) {
        check(deviceDbInfo.uuid.isNotEmpty())

        if (deviceDbInfo.deviceInfo.token == createObject.tokens[deviceDbInfo.uuid])
            return

        createObject.tokens[deviceDbInfo.uuid] = deviceDbInfo.deviceInfo.token
        addValue("$key/tokens/${deviceDbInfo.uuid}", deviceDbInfo.deviceInfo.token)
    }

    override fun deleteFromParent() = check(remoteProjectRecord.userRecords.remove(id) == this)
}
