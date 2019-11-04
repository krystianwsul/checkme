package com.krystianwsul.common.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RemoteRootUserRecord
import java.util.*

class RemoteFriendManager(private val domainFactory: DomainFactory, children: Iterable<DataSnapshot>) {

    var isSaved = false

    val remoteRootUserRecords = children.map { RemoteRootUserRecord(false, it.getValue(UserWrapper::class.java)!!) }.associateBy { it.id }

    fun save() {
        val values = HashMap<String, Any?>()

        remoteRootUserRecords.values.forEach { it.getValues(values) }

        if (values.isNotEmpty()) {
            check(!isSaved)

            isSaved = true
            AndroidDatabaseWrapper.updateFriends(values).checkError(domainFactory, "RemoteFriendManager.save")
        }
    }
}
