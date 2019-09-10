package com.krystianwsul.checkme.firebase

import android.text.TextUtils

import com.krystianwsul.common.firebase.records.RemoteProjectUserRecord


class RemoteProjectUser(
        private val remoteProject: RemoteSharedProject,
        private val remoteProjectUserRecord: RemoteProjectUserRecord) {

    val id = remoteProjectUserRecord.id

    var name
        get() = remoteProjectUserRecord.name
        set(name) {
            check(!TextUtils.isEmpty(name))

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

    fun setToken(token: String?, uuid: String) = remoteProjectUserRecord.setToken(token, uuid)
}
