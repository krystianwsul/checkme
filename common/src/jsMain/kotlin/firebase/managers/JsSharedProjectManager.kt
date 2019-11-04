package firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.RemoteSharedProjectManager
import com.krystianwsul.common.firebase.records.RemoteSharedProjectRecord

class JsSharedProjectManager(
        override val databaseWrapper: DatabaseWrapper,
        jsonWrappers: Map<String, JsonWrapper>
) : RemoteSharedProjectManager() {

    override val remoteProjectRecords = jsonWrappers.entries
            .associate { it.key to RemoteSharedProjectRecord(databaseWrapper, this, it.key, it.value) }
            .toMutableMap()

    override fun getDatabaseCallback(): DatabaseCallback {
        return { message, _, _ -> ErrorLogger.instance.log(message) }
    }
}