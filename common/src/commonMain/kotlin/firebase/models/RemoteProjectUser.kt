package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.records.RemoteProjectUserRecord


class RemoteProjectUser(
        private val remoteProject: SharedProject,
        private val remoteProjectUserRecord: RemoteProjectUserRecord
) {

    val id = remoteProjectUserRecord.id

    var name
        get() = remoteProjectUserRecord.name
        set(name) {
            check(name.isNotEmpty())

            remoteProjectUserRecord.name = name
        }

    val email = remoteProjectUserRecord.email

    var photoUrl
        get() = remoteProjectUserRecord.photoUrl
        set(value) {
            check(!value.isNullOrEmpty())

            remoteProjectUserRecord.photoUrl = value
        }

    fun delete() {
        remoteProject.deleteUser(this)

        remoteProjectUserRecord.delete()
    }

    fun setToken(deviceDbInfo: DeviceDbInfo) = remoteProjectUserRecord.setToken(deviceDbInfo)
}
