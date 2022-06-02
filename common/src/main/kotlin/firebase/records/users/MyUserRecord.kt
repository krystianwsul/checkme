package com.krystianwsul.common.firebase.records.users

import com.krystianwsul.common.VersionInfo
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.MyUserProperties
import com.krystianwsul.common.firebase.json.users.ProjectOrdinalEntryJson
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey


class MyUserRecord(
    databaseWrapper: DatabaseWrapper,
    create: Boolean,
    createObject: UserWrapper,
    userKey: UserKey,
) : RootUserRecord(databaseWrapper, create, createObject, userKey), MyUserProperties {

    private val tokenDelegate = TokenDelegate("$key/$USER_DATA", userJson, ::addValue)

    override fun setToken(deviceDbInfo: DeviceDbInfo, versionInfo: VersionInfo) =
        tokenDelegate.setToken(deviceDbInfo, versionInfo)

    override var photoUrl by Committer(userJson::photoUrl, "$key/$USER_DATA")

    fun addOrdinalEntry(projectKey: ProjectKey.Shared, projectOrdinalEntryJson: ProjectOrdinalEntryJson) {
        val ordinalEntryId = databaseWrapper.newProjectOrdinalEntryId(userKey, projectKey)

        userWrapper.ordinalEntries.getOrPut(projectKey.key) { mutableMapOf() }[ordinalEntryId] = projectOrdinalEntryJson

        addValue("$key/$ORDINAL_ENTRIES/${projectKey.key}/$ordinalEntryId", projectOrdinalEntryJson)
    }
}
