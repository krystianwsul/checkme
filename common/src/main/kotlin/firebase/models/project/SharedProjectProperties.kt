package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.firebase.models.users.ProjectUser

interface SharedProjectProperties {

    val name: String

    val users: Collection<ProjectUser>
}