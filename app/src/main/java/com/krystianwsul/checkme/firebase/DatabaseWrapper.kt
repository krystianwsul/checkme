package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.records.*


object DatabaseWrapper {

    private const val USERS_KEY = "users"
    private const val RECORDS_KEY = "records"

    lateinit var root: String
        private set

    private lateinit var rootReference: DatabaseReference

    fun getRootRecordId() = rootReference.child(RECORDS_KEY).push().key!!

    fun initialize(myApplication: MyApplication) {
        root = myApplication.resources.getString(R.string.firebase_root)
        check(!TextUtils.isEmpty(root))

        rootReference = FirebaseDatabase.getInstance().reference.child(root)!!
    }

    fun setUserInfo(userInfo: UserInfo, uuid: String) {
        val key = userInfo.key

        rootReference.child("$USERS_KEY/$key/userData").updateChildren(userInfo.getValues(uuid))
    }

    fun getUserDataDatabaseReference(key: String) = rootReference.child("$USERS_KEY/$key/userData")!!

    fun addFriend(userInfo: UserInfo, friendUserData: UserData) {
        val myKey = userInfo.key
        val friendKey = friendUserData.key

        rootReference.child("$USERS_KEY/$friendKey/friendOf/$myKey").setValue(true)
    }

    fun getFriendsQuery(userInfo: UserInfo): Query {
        val key = userInfo.key

        return rootReference.child(USERS_KEY).orderByChild("friendOf/$key").equalTo(true)!!
    }

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

    fun getTaskRecordsQuery(userInfo: UserInfo): Query {
        val key = userInfo.key

        return rootReference.child(RECORDS_KEY)
                .orderByChild("recordOf/$key")
                .equalTo(true)!!
    }

    fun updateRecords(values: Map<String, Any?>) = rootReference.child(RECORDS_KEY).updateChildren(values)!!

    fun updateFriends(values: Map<String, Any?>) = rootReference.child(USERS_KEY).updateChildren(values)!!

    fun getUserQuery(userInfo: UserInfo): Query {
        val key = userInfo.key
        check(!TextUtils.isEmpty(key))

        return rootReference.child("$USERS_KEY/$key")!!
    }
}
