package firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RemoteRootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord

class JsRootUserManager(
        override val databaseWrapper: DatabaseWrapper,
        userWrappers: Map<String, UserWrapper>
) : RemoteRootUserManager() {

    override val remoteRootUserRecords = userWrappers.map { RootUserRecord(false, it.value) }.associateBy { it.id }
}