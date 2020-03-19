package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey

class StrangerProjectManager {

    private var strangerProjects: Pair<ProjectKey<*>, List<Pair<UserKey, Boolean>>>? = null

    fun save(domainFactory: DomainFactory) {
        val values = mutableMapOf<String, Any?>()

        strangerProjects?.let { (projectId, userValues) ->
            userValues.forEach { (userId, add) ->
                values["$userId/${RootUserRecord.PROJECTS}/$projectId"] = if (add) true else null
            }
        }
        strangerProjects = null

        MyCrashlytics.log("StrangerProjectManager.save values: $values")

        if (values.isNotEmpty())
            AndroidDatabaseWrapper.updateFriends(values).checkError(domainFactory, "StrangerProjectManager.save")
    }

    fun updateStrangerProjects(
            projectId: ProjectKey<*>,
            addedStrangers: Set<UserKey>,
            removedStrangers: Set<UserKey>
    ) {
        check(strangerProjects == null)

        strangerProjects = Pair(
                projectId,
                addedStrangers.map { it to true } + removedStrangers.map { it to false }
        )
    }
}