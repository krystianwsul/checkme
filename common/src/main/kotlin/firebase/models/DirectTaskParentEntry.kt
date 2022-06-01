package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.utils.ProjectKey

interface DirectTaskParentEntry : TaskParentEntry {

    val projectId: String

    val projectKey: ProjectKey<*>?
}