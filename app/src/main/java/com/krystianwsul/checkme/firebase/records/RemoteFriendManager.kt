package com.krystianwsul.checkme.firebase.records

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.UserWrapper
import com.krystianwsul.checkme.utils.checkError
import java.util.*

class RemoteFriendManager(children: Iterable<DataSnapshot>) {

    var isSaved = false

    val remoteRootUserRecords = children.map { RemoteRootUserRecord(false, it.getValue(UserWrapper::class.java)!!) }.associateBy { it.id }

    fun save() {
        val values = HashMap<String, Any?>()

        remoteRootUserRecords.values.forEach { it.getValues(values) }

        Log.e("asdf", "RemoteFriendManager.save values: $values")

        if (!values.isEmpty()) {
            check(!isSaved)

            isSaved = true
            DatabaseWrapper.updateFriends(values).checkError("RemoteFriendManager.save")
        }
    }
}
