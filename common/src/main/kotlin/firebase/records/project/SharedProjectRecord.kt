package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.firebase.json.users.UserJson
import com.krystianwsul.common.firebase.records.users.ProjectUserRecord
import com.krystianwsul.common.utils.UserKey

interface SharedProjectRecord {

    companion object {

        fun <T : ProjectUserRecord> parseUserJsons(
            users: Map<String, UserJson>,
            newProjectUserRecord: (UserJson) -> T
        ) = users.entries.associate { (id, userJson) ->
            check(id.isNotEmpty())

            UserKey(id) to newProjectUserRecord(userJson)
        }
    }

    val name: String

    val userRecords: Map<UserKey, ProjectUserRecord>
}