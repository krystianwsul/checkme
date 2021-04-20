package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.records.ProjectUserRecord


class ProjectUser(
        private val sharedProject: SharedProject,
        private val projectUserRecord: ProjectUserRecord
) {

    val id = projectUserRecord.id

    var name
        get() = projectUserRecord.name
        set(name) {
            check(name.isNotEmpty())

            projectUserRecord.name = name
        }

    val email = projectUserRecord.email

    var photoUrl
        get() = projectUserRecord.photoUrl
        set(value) {
            check(!value.isNullOrEmpty())

            projectUserRecord.photoUrl = value
        }

    fun delete() {
        sharedProject.deleteUser(this)

        projectUserRecord.delete()
    }

    fun setToken(deviceDbInfo: DeviceDbInfo) = projectUserRecord.setToken(deviceDbInfo)
}
