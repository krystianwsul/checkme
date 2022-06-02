package com.krystianwsul.common.firebase.records.users

import com.krystianwsul.common.VersionInfo
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

    private val tokenDelegate = TokenDelegate(key, createObject, ::addValue)

    fun setToken(deviceDbInfo: DeviceDbInfo, versionInfo: VersionInfo) =
        tokenDelegate.setToken(deviceDbInfo, versionInfo)

    override fun deleteFromParent() = check(remoteProjectRecord.userRecords.remove(id) == this)
}
