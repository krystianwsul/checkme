package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.users.RootUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey

class StrangerProjectManager {

    private var strangerProjects: Pair<ProjectKey<*>, List<Pair<UserKey, Boolean>>>? = null

    fun save(values: MutableMap<String, Any?>) { // todo unify with *RecordManager
        val myValues = mutableMapOf<String, Any?>()

        strangerProjects?.let { (projectId, userValues) ->
            userValues.forEach { (userId, add) ->
                myValues["$userId/${RootUserRecord.PROJECTS}/$projectId"] = if (add) true else null
            }
        }
        strangerProjects = null

        if (myValues.isNotEmpty())
            MyCrashlytics.log("StrangerProjectManager.save values: $myValues")

        values += myValues.mapKeys { "${DatabaseWrapper.USERS_KEY}/${it.key}" }
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