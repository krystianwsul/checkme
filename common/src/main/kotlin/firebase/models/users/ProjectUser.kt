package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.firebase.records.users.ProjectUserRecord


open class ProjectUser(private val projectUserRecord: ProjectUserRecord) {

    val id = projectUserRecord.id

    open val name get() = projectUserRecord.name

    val email get() = projectUserRecord.email

    open val photoUrl get() = projectUserRecord.photoUrl
}
