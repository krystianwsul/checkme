package com.krystianwsul.checkme.firebase

import android.text.TextUtils

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.firebase.json.UserJson
import com.krystianwsul.checkme.firebase.records.RemoteFriendManager

import junit.framework.Assert

class RemoteFriendFactory(children: Iterable<DataSnapshot>) {

    private val remoteFriendManager = RemoteFriendManager(children)

    private val _friends = remoteFriendManager.mRemoteRootUserRecords
            .values
            .map { RemoteRootUser(it) }
            .associateBy { it.id }
            .toMutableMap()

    val isSaved get() = remoteFriendManager.isSaved

    val friends: Collection<RemoteRootUser> get() = _friends.values

    fun save() {
        Assert.assertTrue(!remoteFriendManager.isSaved)

        remoteFriendManager.save()
    }

    fun getUserJsons(friendIds: Set<String>): Map<String, UserJson> {
        Assert.assertTrue(friendIds.all { _friends.containsKey(it) })

        return _friends.entries
                .filter { friendIds.contains(it.key) }
                .associateBy({ it.key }, { it.value.userJson })
    }

    fun getFriend(friendId: String): RemoteRootUser {
        Assert.assertTrue(_friends.containsKey(friendId))

        return _friends[friendId]!!
    }

    fun removeFriend(userKey: String, friendId: String) {
        Assert.assertTrue(!TextUtils.isEmpty(userKey))
        Assert.assertTrue(!TextUtils.isEmpty(friendId))
        Assert.assertTrue(_friends.containsKey(friendId))

        _friends[friendId]!!.removeFriend(userKey)

        _friends.remove(friendId)
    }
}
