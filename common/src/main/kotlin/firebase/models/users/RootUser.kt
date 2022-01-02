package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.firebase.RootUserProperties
import com.krystianwsul.common.firebase.models.cache.ClearableInvalidatableManager
import com.krystianwsul.common.firebase.records.users.RootUserRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey


open class RootUser(private val remoteRootUserRecord: RootUserRecord) :
        RootUserProperties by remoteRootUserRecord,
        JsonTime.UserCustomTimeProvider {

    protected open val _customTimes: MutableMap<CustomTimeId.User, out Time.Custom.User> =
        remoteRootUserRecord.customTimeRecords
            .mapValues { Time.Custom.User(this, it.value) }
            .toMutableMap()

    open val customTimes: Map<CustomTimeId.User, Time.Custom.User> get() = _customTimes

    val clearableInvalidatableManager = ClearableInvalidatableManager()

    protected val projectOrdinalManagers = mutableMapOf<ProjectKey.Shared, ProjectOrdinalManager>()

    val allProjectOrdinalManagers: Collection<ProjectOrdinalManager> = projectOrdinalManagers.values

    fun deleteCustomTime(customTime: Time.Custom.User) {
        check(_customTimes.containsKey(customTime.id))

        _customTimes.remove(customTime.id)
    }

    override fun tryGetUserCustomTime(userCustomTimeKey: CustomTimeKey.User) = customTimes[userCustomTimeKey.customTimeId]
}