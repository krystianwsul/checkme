package firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.managers.RemotePrivateProjectManager
import com.krystianwsul.common.firebase.records.RemotePrivateProjectRecord

class JsPrivateProjectManager(
        override val databaseWrapper: DatabaseWrapper,
        privateProjectJsons: Map<String, PrivateProjectJson>
) : RemotePrivateProjectManager() {

    override val remotePrivateProjectRecords = privateProjectJsons.map { RemotePrivateProjectRecord(databaseWrapper, it.key, it.value) }

    override fun getDatabaseCallback(values: Any): DatabaseCallback {
        return { message, _, _ ->
            ErrorLogger.instance.log(message)
        }
    }
}