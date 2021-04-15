package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.utils.ProjectKey

class JsPrivateProjectManager(
        override val databaseWrapper: DatabaseWrapper,
        privateProjectJsons: Map<String, PrivateProjectJson>
) : PrivateProjectManager() {

    init {
        setInitialValue(
                privateProjectJsons.map { PrivateProjectRecord(databaseWrapper, ProjectKey.Private(it.key), it.value) }
        )
    }
}