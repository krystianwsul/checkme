package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

class JsSharedProjectManager(databaseWrapper: DatabaseWrapper, jsonWrappers: Map<String, JsonWrapper>) :
    SharedProjectManager(databaseWrapper) {

    init {
        setInitialRecords(
            jsonWrappers.entries.associate {
                val projectKey = ProjectKey.Shared(it.key)
                projectKey to SharedProjectRecord(this, projectKey, it.value)
            }
        )
    }
}