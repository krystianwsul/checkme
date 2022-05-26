package com.krystianwsul.common.firebase.records.users

import com.krystianwsul.common.firebase.json.users.UserJson
import com.krystianwsul.common.firebase.records.project.SharedForeignProjectRecord


class ForeignProjectUserRecord(
    remoteProjectRecord: SharedForeignProjectRecord,
    createObject: UserJson
) : ProjectUserRecord(false, remoteProjectRecord, createObject) {

    override val name get() = createObject.name

    override val photoUrl get() = createObject.photoUrl

    override fun deleteFromParent() = throw UnsupportedOperationException()
}
