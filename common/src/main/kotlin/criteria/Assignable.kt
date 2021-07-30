package com.krystianwsul.common.criteria

import com.krystianwsul.common.firebase.models.MyUser
import com.krystianwsul.common.firebase.models.ProjectUser
import com.krystianwsul.common.time.ExactTimeStamp

interface Assignable {

    fun getAssignedTo(now: ExactTimeStamp.Local): List<ProjectUser>

    fun isAssignedToMe(now: ExactTimeStamp.Local, myUser: MyUser) =
            getAssignedTo(now).let { it.isEmpty() || it.any { it.id == myUser.userKey } }
}