package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.androidhuman.rxfirebase2.database.childEvents
import com.androidhuman.rxfirebase2.database.data
import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.FirebaseDatabase
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.records.*


object DatabaseWrapper {

    private const val USERS_KEY = "users"
    private const val RECORDS_KEY = "records"

    val root: String by lazy {
        MyApplication.instance
                .resources
                .getString(R.string.firebase_root)
    }

    private val userInfo get() = MyApplication.instance.userInfo

    private val rootReference by lazy {
        FirebaseDatabase.getInstance()
                .reference
                .child(root)
    }

    fun getRootRecordId() = rootReference.child(RECORDS_KEY).push().key!!

    fun setUserInfo(userInfo: UserInfo, uuid: String) {
        val key = userInfo.key

        rootReference.child("$USERS_KEY/$key/userData").updateChildren(userInfo.getValues(uuid))
    }

    fun getUserDataDatabaseReference(key: String) = rootReference.child("$USERS_KEY/$key/userData")

    fun addFriend(friendKey: String) {
        val myKey = userInfo.key

        rootReference.child("$USERS_KEY/$friendKey/friendOf/$myKey").setValue(true)
    }

    fun addFriends(friendKeys: Set<String>) {
        val myKey = userInfo.key

        val values = friendKeys.map { "$it/friendOf/$myKey" to true }.toMap()
        rootReference.child(USERS_KEY).updateChildren(values)
    }

    fun getFriendSingle(key: String) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/$key")
            .equalTo(true)
            .data()

    fun getFriendObservable(key: String) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/$key")
            .equalTo(true)
            .dataChanges()

    fun getScheduleRecordId(projectId: String, taskId: String): String {
        val id = rootReference.child("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskRecord.TASKS}/$taskId/${RemoteScheduleRecord.SCHEDULES}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    fun getTaskRecordId(projectId: String): String {
        val id = rootReference.child("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskRecord.TASKS}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    fun getTaskHierarchyRecordId(projectId: String): String {
        val id = rootReference.child("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskHierarchyRecord.TASK_HIERARCHIES}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    fun getCustomTimeRecordId(projectId: String): String {
        val id = rootReference.child("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteCustomTimeRecord.CUSTOM_TIMES}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    private fun taskQuery(key: String) = rootReference.child(RECORDS_KEY)
            .orderByChild("recordOf/$key")
            .equalTo(true)

    fun getTaskSingle(key: String) = taskQuery(key).data()

    fun getTaskEvents(key: String) = taskQuery(key).childEvents()

    fun updateRecords(values: Map<String, Any?>) = rootReference.child(RECORDS_KEY).updateChildren(values)

    fun updateFriends(values: Map<String, Any?>) = rootReference.child(USERS_KEY).updateChildren(values)

    fun getUserSingle(key: String) = rootReference.child("$USERS_KEY/$key").data()

    fun getUserObservable(key: String) = rootReference.child("$USERS_KEY/$key").dataChanges()
}
