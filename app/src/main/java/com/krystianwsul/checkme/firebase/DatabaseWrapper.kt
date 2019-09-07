package com.krystianwsul.checkme.firebase

import android.text.TextUtils
import com.androidhuman.rxfirebase2.database.childEvents
import com.androidhuman.rxfirebase2.database.data
import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.FirebaseDatabase
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.records.*


object DatabaseWrapper {

    private const val USERS_KEY = "users"
    private const val RECORDS_KEY = "records"
    private const val PRIVATE_PROJECTS_KEY = "privateProjects"

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

    fun newSharedProjectRecordId() = rootReference.child(RECORDS_KEY).push().key!!

    fun getUserDataDatabaseReference(key: String) = rootReference.child("$USERS_KEY/$key/userData")

    fun addFriend(friendKey: String) = rootReference.child("$USERS_KEY/$friendKey/friendOf/${userInfo.key}").setValue(true)

    fun addFriends(friendKeys: Set<String>) = rootReference.child(USERS_KEY).updateChildren(friendKeys.map { "$it/friendOf/${userInfo.key}" to true }.toMap())

    fun getFriendSingle(key: String) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/$key")
            .equalTo(true)
            .data()

    fun getFriendObservable(key: String) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/$key")
            .equalTo(true)
            .dataChanges()

    fun getPrivateScheduleRecordId(projectId: String, taskId: String): String {
        val id = rootReference.child("$PRIVATE_PROJECTS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskRecord.TASKS}/$taskId/${RemoteScheduleRecord.SCHEDULES}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    fun newSharedScheduleRecordId(projectId: String, taskId: String): String {
        val id = rootReference.child("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskRecord.TASKS}/$taskId/${RemoteScheduleRecord.SCHEDULES}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    fun getPrivateTaskRecordId(projectId: String): String {
        val id = rootReference.child("$PRIVATE_PROJECTS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskRecord.TASKS}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    fun newSharedTaskRecordId(projectId: String): String {
        val id = rootReference.child("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskRecord.TASKS}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    fun getPrivateTaskHierarchyRecordId(projectId: String): String {
        val id = rootReference.child("$PRIVATE_PROJECTS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskHierarchyRecord.TASK_HIERARCHIES}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    fun newSharedTaskHierarchyRecordId(projectId: String): String {
        val id = rootReference.child("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteTaskHierarchyRecord.TASK_HIERARCHIES}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    fun getPrivateCustomTimeRecordId(projectId: String): String {
        val id = rootReference.child("$PRIVATE_PROJECTS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteCustomTimeRecord.CUSTOM_TIMES}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    fun newSharedCustomTimeRecordId(projectId: String): String {
        val id = rootReference.child("$RECORDS_KEY/$projectId/${RemoteProjectRecord.PROJECT_JSON}/${RemoteCustomTimeRecord.CUSTOM_TIMES}")
                .push()
                .key!!
        check(!TextUtils.isEmpty(id))

        return id
    }

    private fun sharedProjectQuery(key: String) = rootReference.child(RECORDS_KEY)
            .orderByChild("recordOf/$key")
            .equalTo(true)

    fun getSharedProjectSingle(key: String) = sharedProjectQuery(key).data()

    fun getSharedProjectEvents(key: String) = sharedProjectQuery(key).childEvents()

    fun updateRecords(values: Map<String, Any?>) = rootReference.child(RECORDS_KEY).updateChildren(values)

    private fun privateProjectQuery(key: String) = rootReference.child("$PRIVATE_PROJECTS_KEY/$key")

    fun getPrivateProjectSingle(key: String) = privateProjectQuery(key).data()

    fun getPrivateProjectObservable(key: String) = privateProjectQuery(key).dataChanges()

    fun updatePrivateProject(values: Map<String, Any?>) = rootReference.child(PRIVATE_PROJECTS_KEY).updateChildren(values)

    fun updateFriends(values: Map<String, Any?>) = rootReference.child(USERS_KEY).updateChildren(values)

    fun getUserSingle(key: String) = rootReference.child("$USERS_KEY/$key").data()

    fun getUserObservable(key: String) = rootReference.child("$USERS_KEY/$key").dataChanges()
}
