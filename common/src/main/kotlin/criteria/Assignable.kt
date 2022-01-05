package com.krystianwsul.common.criteria

import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.time.ExactTimeStamp

interface Assignable {

    fun getAssignedTo(now: ExactTimeStamp.Local): List<ProjectUser>

    fun isAssignedToMe(now: ExactTimeStamp.Local, myUser: MyUser) =
            getAssignedTo(now).let { it.isEmpty() || it.any { it.id == myUser.userKey } }
}