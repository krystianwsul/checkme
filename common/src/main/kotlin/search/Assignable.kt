package com.krystianwsul.common.criteria

import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.firebase.models.users.OwnedProjectUser

interface Assignable {

    fun getAssignedTo(): Collection<OwnedProjectUser>

    fun isAssignedToMe(myUser: MyUser) = getAssignedTo().let { it.isEmpty() || it.any { it.id == myUser.userKey } }
}