package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.firebase.json.UserJson
import com.krystianwsul.checkme.firebase.records.RemoteFriendManager

class RemoteFriendFactory(children: Iterable<DataSnapshot>) {

    private val remoteFriendManager = RemoteFriendManager(children)

    private val _friends = remoteFriendManager.remoteRootUserRecords
            .values
            .map { RemoteRootUser(it) }
            .associateBy { it.id }
            .toMutableMap()

    val isSaved get() = remoteFriendManager.isSaved

    val friends: Collection<RemoteRootUser> get() = _friends.values

    fun save() {
        check(!remoteFriendManager.isSaved)

        remoteFriendManager.save()
    }

    fun getUserJsons(friendIds: Set<String>): MutableMap<String, UserJson> {
        check(friendIds.all { _friends.containsKey(it) })

        return _friends.entries
                .filter { friendIds.contains(it.key) }
                .associateBy({ it.key }, { it.value.userJson })
                .toMutableMap()
    }

    fun getFriend(friendId: String): RemoteRootUser {
        check(_friends.containsKey(friendId))

        return _friends[friendId]!!
    }

    fun removeFriend(userKey: String, friendId: String) {
        check(!TextUtils.isEmpty(userKey))
        check(!TextUtils.isEmpty(friendId))
        check(_friends.containsKey(friendId))

        _friends[friendId]!!.removeFriend(userKey)

        _friends.remove(friendId)
    }
}
