package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.firebase.records.project.PrivateOwnedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

class JsPrivateProjectManager(privateProjectJsons: Map<String, PrivateOwnedProjectJson>) : PrivateProjectManager() {

    init {
        setInitialValue(
            privateProjectJsons.map { PrivateOwnedProjectRecord(ProjectKey.Private(it.key), it.value) }
        )
    }
}