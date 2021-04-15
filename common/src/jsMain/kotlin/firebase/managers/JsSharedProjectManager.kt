package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

class JsSharedProjectManager(
        override val databaseWrapper: DatabaseWrapper,
        jsonWrappers: Map<String, JsonWrapper>,
) : SharedProjectManager() {

    init {
        setInitialRecords(
                jsonWrappers.entries.associate {
                    val projectKey = ProjectKey.Shared(it.key)
                    projectKey to SharedProjectRecord(databaseWrapper, this, projectKey, it.value)
                }
        )
    }
}