package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RemoteRootUserRecord

class RemoteFriendManager(private val domainFactory: DomainFactory, children: Iterable<DataSnapshot>) {

    var isSaved = false

    val remoteRootUserRecords = children.map { RemoteRootUserRecord(false, it.getValue(UserWrapper::class.java)!!) }.associateBy { it.id }

    private var strangerProjects: Pair<String, List<Pair<String, Boolean>>>? = null

    fun save(): Boolean {
        val values = mutableMapOf<String, Any?>()

        remoteRootUserRecords.values.forEach { it.getValues(values) }

        if (values.isNotEmpty()) {
            check(!isSaved)

            isSaved = true
        }

        strangerProjects?.let { (projectId, userValues) ->
            userValues.forEach { (userId, add) ->
                values["$userId/${RemoteRootUserRecord.PROJECTS}/$projectId"] = if (add) true else null
            }
        }
        strangerProjects = null

        MyCrashlytics.log("RemoteFriendManager.save values: $values")

        if (values.isNotEmpty())
            AndroidDatabaseWrapper.updateFriends(values).checkError(domainFactory, "RemoteFriendManager.save")

        return isSaved
    }

    fun updateStrangerProjects(projectId: String, addedStrangers: Set<String>, removedStrangers: Set<String>) {
        check(strangerProjects == null)

        strangerProjects = Pair(
                projectId,
                addedStrangers.map { it to true } + removedStrangers.map { it to false }
        )
    }
}
