package com.krystianwsul.common.firebase.records.users

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

    companion object {

        const val ORDINAL_ENTRIES = "ordinalEntries"
    }

    override fun setToken(deviceDbInfo: DeviceDbInfo) {
        if (deviceDbInfo.token == userJson.tokens[deviceDbInfo.uuid]) return

        userJson.tokens[deviceDbInfo.uuid] = deviceDbInfo.token

        addValue("$key/$USER_DATA/tokens/${deviceDbInfo.uuid}", deviceDbInfo.token)
        addValue("$key/$USER_DATA/uid", deviceDbInfo.userInfo.uid)
    }

    override var photoUrl by Committer(userJson::photoUrl, "$key/$USER_DATA")

    fun addOrdinalEntry(projectKey: ProjectKey.Shared, projectOrdinalEntryJson: ProjectOrdinalEntryJson) {
        val ordinalEntryId = databaseWrapper.newProjectOrdinalEntryId(userKey, projectKey)

        userWrapper.ordinalEntries.getOrPut(projectKey.key) { mutableMapOf() }[ordinalEntryId] = projectOrdinalEntryJson

        addValue("$key/${ORDINAL_ENTRIES}/${projectKey.key}/$ordinalEntryId", projectOrdinalEntryJson)
    }
}
