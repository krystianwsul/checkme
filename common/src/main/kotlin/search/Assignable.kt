package com.krystianwsul.common.criteria

import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.firebase.models.users.ProjectUser

interface Assignable {

    fun getAssignedTo(): Collection<ProjectUser>

    fun isAssignedToMe(myUser: MyUser) = getAssignedTo().let { it.isEmpty() || it.any { it.id == myUser.userKey } }
}