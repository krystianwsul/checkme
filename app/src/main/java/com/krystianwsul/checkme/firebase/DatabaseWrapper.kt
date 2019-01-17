package com.krystianwsul.checkme.firebase

import android.text.TextUtils
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

    fun addFriend(userInfo: UserInfo, friendKey: String) {
        val myKey = userInfo.key

        rootReference.child("$USERS_KEY/$friendKey/friendOf/$myKey").setValue(true)
    }

    fun addFriends(userInfo: UserInfo, friendKeys: Set<String>) {
        val myKey = userInfo.key

        val values = friendKeys.map { "$it/friendOf/$myKey" to true }.toMap()
        rootReference.child(USERS_KEY).updateChildren(values)
    }

    fun friends(userInfo: UserInfo) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/${userInfo.key}")
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

    fun taskRecords(userInfo: UserInfo) = rootReference.child(RECORDS_KEY)
            .orderByChild("recordOf/${userInfo.key}")
            .equalTo(true)
            .dataChanges()

    fun updateRecords(values: Map<String, Any?>) = rootReference.child(RECORDS_KEY).updateChildren(values)

    fun updateFriends(values: Map<String, Any?>) = rootReference.child(USERS_KEY).updateChildren(values)

    fun user(userInfo: UserInfo) = rootReference.child("$USERS_KEY/${userInfo.key}").dataChanges()
}
