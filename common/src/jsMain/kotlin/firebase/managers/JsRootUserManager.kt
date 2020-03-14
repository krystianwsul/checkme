package firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RemoteRootUserManager
import com.krystianwsul.common.firebase.records.RemoteRootUserRecord

class JsRootUserManager(
        override val databaseWrapper: DatabaseWrapper,
        userWrappers: Map<String, UserWrapper>
) : RemoteRootUserManager() {

    override val remoteRootUserRecords = userWrappers.map { RemoteRootUserRecord(false, it.value) }.associateBy { it.id }

    override var saveCallback: (() -> Unit)? = null

    override fun getDatabaseCallback(): DatabaseCallback {
        return { message, _, _ ->
            ErrorLogger.instance.log(message)
            saveCallback?.invoke()
        }
    }
}