package com.krystianwsul.checkme.firebase

import android.text.TextUtils

import com.krystianwsul.checkme.firebase.records.RemoteProjectUserRecord

import junit.framework.Assert

class RemoteProjectUser(private val remoteProject: RemoteProject, private val remoteProjectUserRecord: RemoteProjectUserRecord) {

    val id by lazy { remoteProjectUserRecord.id }

    var name
        get() = remoteProjectUserRecord.name
        set(name) {
            Assert.assertTrue(!TextUtils.isEmpty(name))

            remoteProjectUserRecord.name = name
        }

    val email get() = remoteProjectUserRecord.email

    fun delete() {
        remoteProject.deleteUser(this)

        remoteProjectUserRecord.delete()
    }

    fun setToken(token: String?, uuid: String) = remoteProjectUserRecord.setToken(token, uuid)
}
