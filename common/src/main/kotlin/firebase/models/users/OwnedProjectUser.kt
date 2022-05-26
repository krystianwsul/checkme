package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.project.SharedOwnedProject
import com.krystianwsul.common.firebase.records.users.OwnedProjectUserRecord


class OwnedProjectUser(
    private val sharedProject: SharedOwnedProject,
    private val projectUserRecord: OwnedProjectUserRecord,
) : ProjectUser(projectUserRecord) {

    override var name
        get() = super.name
        set(name) {
            check(name.isNotEmpty())

            projectUserRecord.name = name
        }

    override var photoUrl
        get() = super.photoUrl
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
