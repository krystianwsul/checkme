package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ObserverHolder
import com.krystianwsul.checkme.firebase.json.UserJson
import com.krystianwsul.checkme.firebase.records.RemoteFriendManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import java.util.*

class RemoteFriendFactory(children: Iterable<DataSnapshot>) {

    companion object {

        private var instance: RemoteFriendFactory? = null

        private var compositeDisposable = CompositeDisposable()

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
        fun clearListener() = compositeDisposable.clear()

        @Synchronized
        fun setListener() {
            DatabaseWrapper.friends
                    .subscribe {
                        Log.e("asdf", "RemoteFriendFactory.onDataChange, dataSnapshot: $it")

                        setFriendRecords(it)
                    }
                    .addTo(compositeDisposable)
        }

        @Synchronized
        private fun setFriendRecords(dataSnapshot: DataSnapshot) {
            RemoteFriendFactory.setInstance(RemoteFriendFactory(dataSnapshot.children))

            ObserverHolder.notifyDomainObservers(ArrayList())

            tryNotifyFriendListeners()
        }

        @Synchronized
        fun tryNotifyFriendListeners() {
            if (!DomainFactory.instance.getIsConnected())
                return

            if (!hasFriends())
                return

            friendListeners.forEach { it() }
            friendListeners.clear()
        }

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
