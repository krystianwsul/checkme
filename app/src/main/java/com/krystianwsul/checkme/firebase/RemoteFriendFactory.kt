package com.krystianwsul.checkme.firebase

import android.text.TextUtils

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.firebase.json.UserJson
import com.krystianwsul.checkme.firebase.records.RemoteFriendManager

import junit.framework.Assert

class RemoteFriendFactory(children: Iterable<DataSnapshot>) {

    companion object {

        private var instance: RemoteFriendFactory? = null

        @Synchronized
        fun setInstance(remoteFriendFactory: RemoteFriendFactory?) {
            instance = remoteFriendFactory
        }

        @Synchronized
        fun hasFriends() = instance != null

        @Synchronized
        fun getFriends() = instance!!.friends

        @Synchronized
        fun save() = instance!!.save()

        @Synchronized
        fun isSaved() = instance!!.isSaved

        @Synchronized
        fun removeFriend(userKey: String, friendId: String) = instance!!.removeFriend(userKey, friendId)

        @Synchronized
        fun getFriend(friendId: String) = instance!!.getFriend(friendId)

        @Synchronized
        fun getUserJsons(friendIds: Set<String>) = instance!!.getUserJsons(friendIds)
    }

    private val remoteFriendManager = RemoteFriendManager(children)

    private val _friends = remoteFriendManager.remoteRootUserRecords
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

    fun getUserJsons(friendIds: Set<String>): MutableMap<String, UserJson> {
        Assert.assertTrue(friendIds.all { _friends.containsKey(it) })

        return _friends.entries
                .filter { friendIds.contains(it.key) }
                .associateBy({ it.key }, { it.value.userJson })
                .toMutableMap()
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
