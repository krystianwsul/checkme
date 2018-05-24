package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import android.util.Log
import com.annimon.stream.Stream
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ObserverHolder
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.json.UserJson
import com.krystianwsul.checkme.firebase.records.RemoteFriendManager
import junit.framework.Assert
import java.util.*

class RemoteFriendFactory(children: Iterable<DataSnapshot>) {

    companion object {

        private var instance: RemoteFriendFactory? = null

        private var mFriendQuery: Query? = null

        private var mFriendListener: ValueEventListener? = null

        private val mFriendFirebaseListeners = ArrayList<Function1<DomainFactory, Unit>>()

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
            mFriendQuery!!.removeEventListener(mFriendListener!!)
            mFriendQuery = null
            mFriendListener = null
        }

        @Synchronized
        fun setListener(mUserInfo: UserInfo) {
            mFriendQuery = DatabaseWrapper.getFriendsQuery(mUserInfo)
            mFriendListener = object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    Log.e("asdf", "DomainFactory.mFriendListener.onDataChange, dataSnapshot: " + dataSnapshot)

                    setFriendRecords(dataSnapshot)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("asdf", "DomainFactory.mFriendListener.onCancelled", databaseError.toException())

                    MyCrashlytics.logException(databaseError.toException())
                }
            }
            mFriendQuery!!.addValueEventListener(mFriendListener)
        }

        @Synchronized
        private fun setFriendRecords(dataSnapshot: DataSnapshot) {
            RemoteFriendFactory.setInstance(RemoteFriendFactory(dataSnapshot.children))

            ObserverHolder.notifyDomainObservers(ArrayList())

            tryNotifyFriendListeners()
        }

        @Synchronized
        fun tryNotifyFriendListeners() {
            if (!DomainFactory.getDomainFactory().isConnected)
                return

            if (!RemoteFriendFactory.hasFriends())
                return

            Stream.of<Function1<DomainFactory, Unit>>(mFriendFirebaseListeners).forEach { firebaseListener -> firebaseListener.invoke(DomainFactory.getDomainFactory()) } // todo remove param
            mFriendFirebaseListeners.clear()
        }

        @Synchronized
        fun clearFriendListeners() = mFriendFirebaseListeners.clear()

        @Synchronized
        fun addFriendListener(friendListener: Function1<DomainFactory, Unit>) = mFriendFirebaseListeners.add(friendListener)
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
