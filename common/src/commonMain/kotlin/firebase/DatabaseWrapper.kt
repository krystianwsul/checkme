package com.krystianwsul.common.firebase

abstract class DatabaseWrapper {

    companion object {

        lateinit var instance: DatabaseWrapper
    }

    abstract fun getPrivateScheduleRecordId(projectId: String, taskId: String): String

    abstract fun getPrivateTaskRecordId(projectId: String): String

    abstract fun getPrivateTaskHierarchyRecordId(projectId: String): String

    abstract fun getPrivateCustomTimeRecordId(projectId: String): String
}