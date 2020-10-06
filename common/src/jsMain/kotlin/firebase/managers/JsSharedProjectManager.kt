package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

class JsSharedProjectManager(
        override val databaseWrapper: DatabaseWrapper,
        jsonWrappers: Map<String, JsonWrapper>
) : SharedProjectManager() {

    override var recordPairs = jsonWrappers.entries
            .associate {
                val projectKey = ProjectKey.Shared(it.key)
                projectKey to Pair(SharedProjectRecord(databaseWrapper, this, projectKey, it.value), false)
            }
            .toMutableMap()
}