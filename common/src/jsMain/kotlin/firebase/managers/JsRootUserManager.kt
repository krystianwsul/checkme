package firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RemoteRootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord

class JsRootUserManager(
        override val databaseWrapper: DatabaseWrapper,
        userWrappers: Map<String, UserWrapper>
) : RemoteRootUserManager() {

    override var remoteRootUserRecords = userWrappers.map { RootUserRecord(false, it.value) to false }
            .associateBy { it.first.id }
            .toMutableMap()
}