package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.ObserverHolder
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.json.UserJson
import com.krystianwsul.checkme.firebase.records.RemoteFriendManager
import java.util.*

class RemoteFriendFactory(children: Iterable<DataSnapshot>) {

    companion object {

        private var instance: RemoteFriendFactory? = null

        private var database: Pair<Query, ValueEventListener>? = null

        private val friendListeners = mutableListOf<() -> Unit>()

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

        @Synchronized
        fun clearListener() {
            database!!.apply { first.removeEventListener(second) }
            database = null
        }

        @Synchronized
        fun setListener(mUserInfo: UserInfo) {
            database = Pair(DatabaseWrapper.getFriendsQuery(mUserInfo), object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    Log.e("asdf", "RemoteFriendFactory.onDataChange, dataSnapshot: $dataSnapshot")

                    setFriendRecords(dataSnapshot)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("asdf", "RemoteFriendFactory.onCancelled", databaseError.toException())

                    MyCrashlytics.logException(databaseError.toException())
                }
            }).apply { first.addValueEventListener(second) }
        }

        @Synchronized
        private fun setFriendRecords(dataSnapshot: DataSnapshot) {
            RemoteFriendFactory.setInstance(RemoteFriendFactory(dataSnapshot.children))

            ObserverHolder.notifyDomainObservers(ArrayList())

            tryNotifyFriendListeners()
        }

        @Synchronized
        fun tryNotifyFriendListeners() {
            if (!KotlinDomainFactory.getKotlinDomainFactory().domainFactory.isConnected)
                return

            if (!hasFriends())
                return

            friendListeners.forEach { it() }
            friendListeners.clear()
        }

        @Synchronized
        fun clearFriendListeners() = friendListeners.clear()

        @Synchronized
        fun addFriendListener(friendListener: () -> Unit) = friendListeners.add(friendListener)
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
