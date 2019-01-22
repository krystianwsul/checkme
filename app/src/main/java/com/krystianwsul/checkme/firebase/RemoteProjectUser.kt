package com.krystianwsul.checkme.firebase

import android.text.TextUtils

import com.krystianwsul.checkme.firebase.records.RemoteProjectUserRecord


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

    fun delete() {
        remoteProject.deleteUser(this)

        remoteProjectUserRecord.delete()
    }

    fun setToken(token: String?, uuid: String) = remoteProjectUserRecord.setToken(token, uuid)
}
